package org.embl.mobie.viewer.table;

import com.google.common.collect.Streams;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.select.Listeners;
import de.embl.cba.tables.tablerow.TableRow;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
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

	public void addColumn( String columnName, Object defaultValue )
	{
		final int rowCount = getRowCount();

		for ( int i = 0; i < rowCount; i++ )
		{
			tableRows.get( i ).setCell( columnName, defaultValue.toString() );
		}
	}

	public void addColumn( String columnName, Object[] values )
	{
		final int rowCount = getRowCount();

		for ( int i = 0; i < rowCount; i++ )
		{
			tableRows.get( i ).setCell( columnName, values[ i ].toString() );
		}
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

	// TODO:
	//  - extends Iterable< T > ? needed?
	//  - addColumn:
	//       assert tableRows.size() == size;
	//
	//			for ( int i = 0; i < size; i++ )
	//			{
	//				tableRows.get( i ).setCell( columnName, values.get( i ) );
	//			}
	//   - we need to support: tableRows.get( rowSorter.convertRowIndexToModel( rowIndex ) );
	//     - e.g. the Annotator needs this to notify the selection model
	//     - the code in segmentation-annotator seems more up to date:
	//         - e.g. check the Annotator class, which uses a TableModel
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
			columnNameToClass.put( columnNames.get( columnIndex ), getTableCellClass( cell ) );
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

	public Class getTableCellClass( String string )
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
		return tableRows.get( rowIndex ).getCell( columnNames.get( columnIndex ) );
	}

	@Override
	public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
		final T tableRow = tableRows.get( rowIndex );
		tableRow.setCell( columnNames.get( columnIndex ), aValue.toString() );
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
