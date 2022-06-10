package org.embl.mobie.viewer.table;

import de.embl.cba.tables.Utils;
import de.embl.cba.tables.tablerow.TableRow;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TableRowsTableModel < T extends TableRow >  implements TableModel
{
	private final List< T > tableRows;
	private List< String > columnNames;
	private ArrayList< Class > columnClasses;

	public TableRowsTableModel( List< T > tableRows )
	{
		this.tableRows = tableRows;
		this.columnNames = tableRows.get( 0 ).getColumnNames().stream().collect( Collectors.toList() );
		setColumns();
	}

	public List< String > getColumnNames()
	{
		return columnNames;
	}

	private void setColumns()
	{
		columnNames = tableRows.get( 0 ).getColumnNames().stream().collect( Collectors.toList() );
		columnClasses = new ArrayList<>(  );

		for ( int column = 0; column < getColumnCount(); column++ )
		{
			final String cell = (String) this.getValueAt( 0, column );
			columnClasses.add( getTableCellClass( cell ) );
		}
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

	@Override
	public int getColumnCount()
	{
		return columnNames.size();
	}

	@Nls
	@Override
	public String getColumnName( int columnIndex )
	{
		return columnNames.get( columnIndex );
	}

	@Override
	public Class< ? > getColumnClass( int columnIndex )
	{
		return columnClasses.get( columnIndex );
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
		if ( tableRow.getColumnNames().size() != columnNames.size() )
			setColumns(); // TODO: and notify listener!
	}

	@Override
	public void addTableModelListener( TableModelListener l )
	{
		// TODO: fire numRows changed?
	}

	@Override
	public void removeTableModelListener( TableModelListener l )
	{

	}
}
