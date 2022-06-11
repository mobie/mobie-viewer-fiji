package org.embl.mobie.viewer.table;

import com.google.common.collect.Streams;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.select.Listeners;
import de.embl.cba.tables.tablerow.TableRow;
import ij.IJ;

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

public class TableRowsTableModel < T extends TableRow >  implements TableModel, Iterable< T >
{
	private List< T > tableRows;
	private Map< String, Class > columnNameToClass;
	protected final Listeners.SynchronizedList< TableModelListener > listeners = new Listeners.SynchronizedList<>( );
	private List< String > columnNames;

	public void addColumn( String columnName, String defaultValue )
	{
		final int rowCount = getRowCount();

		for ( int i = 0; i < rowCount; i++ )
		{
			tableRows.get( i ).setCell( columnName, defaultValue );
		}
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
		{
			tableRows.get( i ).setCell( columnName, values.get( i ).toString() );
		}
	}

	public void mergeColumns( Map< String, List< String > > newColumns, List< String > mergeByColumnNames )
	{
		// TODO:
		//   deal with the fact that the label ids are sometimes
		//   stored as 1 and sometimes as 1.0
		//   after below operation they all will be 1.0, 2.0, ...
		//    MoBIEHelper.toDoubleStrings( segmentIdColumn );
		//    MoBIEHelper.toDoubleStrings( newColumns.get( TableColumnNames.SEGMENT_LABEL_ID ) );

		// create a lookup map for finding the correct row,
		// given reference cell entries
		final HashMap< String, Integer > keyToRowIndex = new HashMap<>();
		final StringBuilder referenceKey = new StringBuilder();
		final int rowCount = getRowCount();
		for ( int rowIndex = 0; rowIndex < rowCount; ++rowIndex )
		{
			final String key = getRowKey( mergeByColumnNames, referenceKey, rowIndex );
			keyToRowIndex.put( key, rowIndex );
		}

		for ( Map.Entry< String, List< String > > newColumn : newColumns.entrySet() )
		{
			final String columnName = newColumn.getKey();
			final List< String > values = newColumn.getValue();

			if ( getColumnNames().contains( columnName ) )
				continue; // those are the columns by which to merge

			// initialise the new columns with a default value
			// - the new cells may have a different order
			// - some cells may keep those default values, because it is
			//   not required that all rows exist in the new columns
			// TODO: like this we may need to iterate twice
			//  maybe better to iterate through the existing rows
			//  and fetch the corresponding row in the new columns?

			final String defaultValue = getDefaultValue( values.get( 0 ) );
			addColumn( columnName, defaultValue  );

			// go through the new columns and put their values
			// into the correct row
			// this could be slow...
			// maybe room for improvements
			// final long start = System.currentTimeMillis();

			final int numNewRows = values.size();
			int numSkippedRows = 0;
			for ( int rowIndex = 0; rowIndex < numNewRows; ++rowIndex )
			{
				final String key = getRowKey( mergeByColumnNames, referenceKey, rowIndex );
				final Integer targetRowIndex = keyToRowIndex.get( key );

				if ( targetRowIndex == null )
				{
					// The key does not exist in the reference table.
					numSkippedRows++;
					continue;
				}
				else
				{
					setValueAt( values.get( rowIndex ), targetRowIndex, columnName );
				}
			}

			//		System.out.println( ( System.currentTimeMillis() - start ) / 1000.0 ) ;

			if ( numSkippedRows > 0 )
				IJ.log("[WARNING] There were "+ numSkippedRows+" rows merging column " + columnName );

		}
	}

	private String getRowKey( List< String > mergeByColumnNames, StringBuilder referenceKey, int rowIndex )
	{
		for ( String columnName : mergeByColumnNames )
			referenceKey.append( getValueAt( rowIndex, columnName ) + " " );
		final String key = referenceKey.toString();
		referenceKey.delete( 0, referenceKey.length() ); // clear for reuse
		return key;
	}

	private String getDefaultValue( String string )
	{
		if ( getCellClass( string ) == Double.class )
			return "NaN"; // for numbers
		else
			return  "None"; // for text
	}

	class TableRowsIterator implements Iterator< T >
	{
		int index = 0;

		TableRowsIterator() {
		}

		public boolean hasNext() {
			return index < getRowCount();
		}

		public T next() {
			return get(index++);
		}
	}

	public TableRowsTableModel( List< T > tableRows )
	{
		this.tableRows = tableRows;
		setColumns();
	}

	public int indexOf( T tableRow )
	{
		return tableRows.indexOf( tableRow );
	}

	public boolean isNumeric( String columnName )
	{
		return columnNameToClass.get( columnName ) == Double.class;
	}

	// it would be better to avoid this and instead use
	// this whole class as a list
	@Deprecated
	public List< T > getTableRows()
	{
		return tableRows;
	}

	private void setColumns()
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
	}

	public List< String > getColumnNames()
	{
		return columnNames;
	}

	public Stream< T > stream()
	{
		final Iterator< T > iterator = tableRows.iterator();
		final Stream< T > stream = Streams.stream( iterator );
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
		return columnNameToClass.size();
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
		setValueAt( value, rowIndex, columnNames.indexOf( columnIndex ) );
	}

	public void setValueAt( Object aValue, int rowIndex, String columnName )
	{
		final T tableRow = tableRows.get( rowIndex );
		tableRow.setCell( columnName, aValue.toString() );
		if ( tableRow.getColumnNames().size() != columnNameToClass.size() )
			setColumns(); // TODO: and notify listener? maybe JTable anyway will be aware of this
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
	public Iterator<T> iterator() {
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

	public T get( int index )
	{
		return tableRows.get( index );
	}
}
