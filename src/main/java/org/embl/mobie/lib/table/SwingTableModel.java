package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;
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
		return tableModel.numAnnotations();
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
		final String columnName = getColumnName( columnIndex );
		final Class< ? > columnClass = tableModel.columnClass( columnName );
		if ( columnClass == null )
			throw new RuntimeException("Could determine the class of column " + columnName );
		return columnClass;
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return false;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		try
		{
			final String columnName = getColumnName( columnIndex );
			final Annotation annotation = tableModel.annotation( rowIndex );
			return annotation.getValue( columnName );
		}
		catch ( Exception e )
		{
			throw( e );
		}
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
