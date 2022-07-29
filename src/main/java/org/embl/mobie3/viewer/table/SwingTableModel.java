package org.embl.mobie3.viewer.table;

import org.embl.mobie3.viewer.annotation.Annotation;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class SwingTableModel implements TableModel
{
	private final AnnotationTableModel< ? extends Annotation > tableModel;

	public < A extends Annotation > SwingTableModel( AnnotationTableModel< A > tableModel )
	{
		this.tableModel = tableModel;
	}

	@Override
	public int getRowCount()
	{
		return tableModel.numRows();
	}

	@Override
	public int getColumnCount()
	{
		return tableModel.columnNames().size();
	}

	@Nls
	@Override
	public String getColumnName( int columnIndex )
	{
		return tableModel.columnNames().get( columnIndex );
	}

	@Override
	public Class< ? > getColumnClass( int columnIndex )
	{
		return tableModel.columnClass( getColumnName( columnIndex ) );
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return false;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		return tableModel.row( rowIndex ).getValue( getColumnName( columnIndex ) );
	}

	@Override
	public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
		throw new UnsupportedOperationException("Setting of values not yet implemented!");
	}

	@Override
	public void addTableModelListener( TableModelListener l )
	{

	}

	@Override
	public void removeTableModelListener( TableModelListener l )
	{

	}
}
