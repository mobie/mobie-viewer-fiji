/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
