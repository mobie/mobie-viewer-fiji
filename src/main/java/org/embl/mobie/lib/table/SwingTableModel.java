package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

public class SwingTableModel implements TableModel
{
	private final AnnotationTableModel< ? extends Annotation > tableModel;
	private final List< TableModelListener > tableModelListeners = new ArrayList<>();

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
		return true;
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
		final Class< ? > columnClass = getColumnClass( columnIndex );
		if ( columnClass.equals( String.class ) )
		{
			final String columnName = getColumnName( columnIndex );
			tableModel.annotations().get( rowIndex ).setString( columnName, aValue.toString() );
		}
	}

	@Override
	public void addTableModelListener( TableModelListener l )
	{
		tableModelListeners.add( l );
	}

	@Override
	public void removeTableModelListener( TableModelListener l )
	{
		tableModelListeners.remove( l );
	}

	public void tableChanged()
	{
		for ( TableModelListener listener : tableModelListeners )
		{
			final TableModelEvent tableModelEvent = new TableModelEvent( this, TableModelEvent.HEADER_ROW );
			listener.tableChanged( tableModelEvent );
		}
	}
}
