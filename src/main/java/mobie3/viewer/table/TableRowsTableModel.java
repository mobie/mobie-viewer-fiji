package mobie3.viewer.table;

import com.google.common.collect.Streams;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.Listeners;
import de.embl.cba.tables.tablerow.TableRow;
import mobie3.viewer.annotation.Region;
import mobie3.viewer.annotation.Annotation;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TableRowsTableModel < A extends Annotation > implements TableModel, Iterable< A >
{
	private List< A > tableRows;
	private Map< String, Class > columnNameToClass;
	protected final Listeners.SynchronizedList< TableModelListener > listeners = new Listeners.SynchronizedList<>( );
	private List< String > columnNames;

	public TableRowsTableModel()
	{
		tableRows = new ArrayList<>();
	}

	public TableRowsTableModel( List< A > tableRows )
	{
		this.tableRows = tableRows;
		configureColumns();
	}

	public void addColumn( String columnName, String value )
	{
		final int rowCount = getRowCount();
		for ( int i = 0; i < rowCount; i++ )
			tableRows.get( i ).setCell( columnName, value );
		configureColumns();
	}

	/**
	 * Assumes that the values in the new column
	 * are sorted correctly with respect to the
	 * already existing columns.
	 *
	 * @param columnName
	 * @param values
	 */
	public void addColumn( String columnName, List< String > values )
	{
		final int rowCount = getRowCount();
		assert rowCount == values.size();
		for ( int i = 0; i < rowCount; i++ )
			tableRows.get( i ).setCell( columnName, values.get( i ) );
		configureColumns();
	}

	/**
	 * TODO: Considerations:
	 *   - for lazy loading from merged grid source
	 *     - the sourceColumns can be much smaller => iterate over them
	 *     - this merging can happen quite often => keep the keyToRow
	 *       list somewhere and update when necessary
	 *     - the sourceColumns will partially already exist
	 *       => do not overwrite the values
	 *     - when the sourceColumns are added for the first time
	 *       (i.e. do not exist) => only then set default value.
	 */
	public void mergeColumns( Map< String, List< String > > sourceColumns )
	{
		// FIXME: (maybe via a listening mechanism?)
		//   - tableViewer.enableRowSorting( false );
		//   - tableViewer.enableRowSorting( true );

		final ArrayList< String > mergeByColumnNames = getMergeByColumnNames();

		// create a lookup maps for finding the correct rows
		// given reference cell entries
		final StringBuilder referenceKey = new StringBuilder();

		final HashMap< String, Integer > keyToTargetRowIndex = new HashMap<>();
		final int targetRowCount = getRowCount();
		for ( int targetRowIndex = 0; targetRowIndex < targetRowCount; ++targetRowIndex )
		{
			for ( String columnName : mergeByColumnNames )
				referenceKey.append( getValueAt( targetRowIndex, columnName ) + " " );
			final String key = referenceKey.toString();
			referenceKey.delete( 0, referenceKey.length() ); // clear for reuse
			keyToTargetRowIndex.put( key, targetRowIndex );
		}

		final HashMap< Integer, Integer > targetRowIndexToSourceRowIndex = new HashMap<>();
		final int sourceRowCount = sourceColumns.values().iterator().next().size();
		for ( int sourceRowIndex = 0; sourceRowIndex < sourceRowCount; ++sourceRowIndex )
		{
			for ( String columnName : mergeByColumnNames )
				referenceKey.append( sourceColumns.get( columnName ).get( sourceRowIndex ) + " " );
			final String key = referenceKey.toString();
			referenceKey.delete( 0, referenceKey.length() ); // clear for reuse

			targetRowIndexToSourceRowIndex.put( keyToTargetRowIndex.get( key ), sourceRowIndex );
		}

		for ( Map.Entry< String, List< String > > newColumn : sourceColumns.entrySet() )
		{
			final String columnName = newColumn.getKey();
			final List< String > values = newColumn.getValue();

			if ( getColumnNames().contains( columnName ) )
				continue; // those are the columns by which to merge

			final String defaultValue = getDefaultValue( values.get( 0 ) );
			for ( int targetRowIndex = 0; targetRowIndex < targetRowCount; ++targetRowIndex )
			{
				final Integer sourceRowIndex = targetRowIndexToSourceRowIndex.get( targetRowIndex );
				if ( sourceRowIndex == null )
				{
					// row does not exist in the new columns
					setValueAt( defaultValue, targetRowIndex, columnName );
				}
				else
				{
					setValueAt( values.get( sourceRowIndex ), targetRowIndex, columnName );
				}
			}
		}
	}

	// TODOL this is specific for the different types => Maybe this should not live here?
	private ArrayList< String > getMergeByColumnNames()
	{
		final ArrayList< String > mergeByColumnNames = new ArrayList<>();
		if ( tableRows.get( 0 ) instanceof ImageSegment )
		{
			mergeByColumnNames.add( ColumnNames.LABEL_ID );
			mergeByColumnNames.add( ColumnNames.LABEL_IMAGE_ID );
		}
		else if ( tableRows.get( 0 ) instanceof Region )
		{
			mergeByColumnNames.add( ColumnNames.REGION_ID );
		}
		return mergeByColumnNames;
	}


	private String getDefaultValue( String string )
	{
		if ( getCellClass( string ) == Double.class )
			return "NaN"; // for numbers
		else
			return "None"; // for text
	}

	public synchronized void addAll( List< A > tableRows )
	{
		if ( this.tableRows.size() == 0 )
		{
			this.tableRows.addAll( tableRows );
			configureColumns();
		}
		else
		{
			this.tableRows.addAll( tableRows );
			// Configure columns again?
			// Check consistency of column names?
		}

		// TODO
//		for ( TableRowImageSegment segment : segmentationDisplay.tableRows )
//		{
//			if ( segment.labelId() == 0 )
//			{
//				throw new UnsupportedOperationException( "The table contains rows (image segments) with label index 0, which is not supported and will lead to errors. Please change the table accordingly." );
//			}
//		}
	}

	class TableRowsIterator implements Iterator< A >
	{
		int index = 0;

		TableRowsIterator() {
		}

		public boolean hasNext() {
			return index < getRowCount();
		}

		public A next() {
			return get(index++);
		}
	}

	// useless we have getTableRows
	public int indexOf( A tableRow )
	{
		return tableRows.indexOf( tableRow );
	}

	public boolean isNumeric( String columnName )
	{
		return columnNameToClass.get( columnName ) == Double.class;
	}

	// it may be better to avoid this and instead use
	// this whole class as a list
	public List< A > getTableRows()
	{
		return tableRows;
	}

	private synchronized void configureColumns()
	{
		columnNames = tableRows.get( 0 ).getColumnNames().stream().collect( Collectors.toList() );
		columnNameToClass = new LinkedHashMap<>();
		final int columnCount = columnNames.size();

		for ( int columnIndex = 0; columnIndex < columnCount; columnIndex++ )
		{
			final String cell = (String) this.getValueAt( 0, columnIndex );
			final Class cellClass = getCellClass( cell );
			columnNameToClass.put( columnNames.get( columnIndex ), cellClass );
		}

		// TODO: notify listeners?
	}

	public List< String > getColumnNames()
	{
		return Collections.unmodifiableList( columnNames );
	}

	public Stream< A > stream()
	{
		final Iterator< A > iterator = tableRows.iterator();
		final Stream< A > stream = Streams.stream( iterator );
		return stream;
	}

	public Class getCellClass( String string )
	{
		try
		{
			Utils.parseDouble( string );
			return Double.class;
		}
		catch ( Exception e )
		{
			return String.class;
		}
	}

	@Override
	public int getRowCount()
	{
		return tableRows.size();
	}

	public int size()
	{
		return getRowCount();
	}

	@Override
	public int getColumnCount()
	{
		return columnNames.size();
	}

	@Override
	public String getColumnName( int columnIndex )
	{
		return columnNames.get( columnIndex );
	}

	@Override
	public Class< ? > getColumnClass( int columnIndex )
	{

		return columnNameToClass.get( columnNames.get( columnIndex ) );
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return true;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		return getValueAt( rowIndex, columnNames.get( columnIndex ) );
	}

	public String getValueAt( int rowIndex, String columnName )
	{
		return tableRows.get( rowIndex ).getCell( columnName );
	}

	@Override
	public void setValueAt( Object value, int rowIndex, int columnIndex )
	{
		setValueAt( value.toString(), rowIndex, columnNames.indexOf( columnIndex ) );
	}

	public synchronized void setValueAt( String value, int rowIndex, String columnName )
	{
		if ( ! getColumnNames().contains( columnName) )
			addColumn( columnName, getDefaultValue( value )  );
		tableRows.get( rowIndex ).setCell( columnName, value.toString() );
	}

	@Override
	public void addTableModelListener( TableModelListener l )
	{
		listeners.add( l );
	}

	@Override
	public void removeTableModelListener( TableModelListener l )
	{

	}

	@Override
	public Iterator< A > iterator() {
		return new TableRowsIterator();
	}

	public void addTableRows( List< TableRow > tableRows )
	{
		tableRows.addAll( tableRows );
	}

	public Listeners< TableModelListener > listeners()
	{
		return listeners;
	}

	public A get( int index )
	{
		return tableRows.get( index );
	}
}
