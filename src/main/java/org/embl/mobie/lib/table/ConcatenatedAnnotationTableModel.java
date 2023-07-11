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

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.io.StorageLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConcatenatedAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A > implements AnnotationListener< A >
{
	private final Set< AnnotationTableModel< A > > tableModels;
	private AnnotationTableModel< A > referenceTable;
	private ArrayList< A > annotations = new ArrayList<>();
	private boolean allTablesLoaded = false;

	public ConcatenatedAnnotationTableModel( Set< AnnotationTableModel< A > > tableModels )
	{
		this.tableModels = tableModels;

		// note that all loading of data from the {@code tableModels}
		// it handled by the listening.
		for ( AnnotationTableModel< A > tableModel : tableModels )
		{
			tableModel.addAnnotationListener( this );
		}

		this.referenceTable = tableModels.iterator().next();
	}

	@Override
	public List< String > columnNames()
	{
		return referenceTable.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return referenceTable.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return referenceTable.columnClass( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return annotations.size();
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return annotations.indexOf( annotation );
	}

	@Override
	public A annotation( int rowIndex )
	{
		// We do not update the tables here,
		// because one should only ask for
		// rows with an index lower than the
		// current numRows.
		return annotations.get( rowIndex );
	}

	@Override
	public void loadTableChunk( String tableChunk )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.loadTableChunk( tableChunk );

		// TODO: it is not logical that this method does not trigger
		//   an annotation listener...
	}

	@Override
	public void loadExternalTableChunk( StorageLocation location )
	{
		throw new UnsupportedOperationException("loadExternalTableChunk is not implemented for " + this.getClass() );
	}

	@Override
	public Collection< String > getAvailableTableChunks()
	{
		return referenceTable.getAvailableTableChunks();
	}

	@Override
	public LinkedHashSet< String > getLoadedTableChunks()
	{
		return referenceTable.getLoadedTableChunks();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public ArrayList< A > annotations()
	{
		return annotations;
	}

	@Override
	public void addStringColumn( String columnName )
	{
		if ( columnNames().contains( columnName ) )
			return;

		for ( AnnotationTableModel< A > tableModel : tableModels )
		{
			if ( ! tableModel.columnNames().contains( columnName ) )
			{
				tableModel.addStringColumn( columnName );
			}
		}

		for ( AnnotationListener< A > listener : listeners.list )
			listener.columnsAdded( null );
	}

	@Override
	public StorageLocation getStorageLocation()
	{
		return referenceTable.getStorageLocation();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.transform( affineTransform3D );
	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{
		listeners.add( listener );
		if( annotations.size() > 0 )
			listener.annotationsAdded( annotations );
	}

	@Override
	public void annotationsAdded( Collection< A > annotations )
	{
		// this method is called, e.g., if
		// {@code Annotations} have been added to (one of)
		// the wrapped
		// {code Set< AnnotationTableModel< A > > tableModels}
		// and should thus be added to this model.
		this.annotations.addAll( annotations );

		// inform listeners such as the {@code TableView}
		for ( AnnotationListener< A > listener : listeners.list )
			listener.annotationsAdded( annotations );
	}

	@Override
	public void columnsAdded( Collection< String > columns )
	{
		// additions of columns occurs via
		// {@code addStringColumn} or
		// {@code loadTableChunk},
		// which notify listeners themselves.
		// notifying listeners here would cause notification
		// from each of the wrapped table model, which is not needed
		// and in fact leads to concurrency errors
		// re-rendering views of this table.
	}

	public synchronized void loadAllTables()
	{
		if ( ! allTablesLoaded )
		{
			for ( AnnotationTableModel< ? extends Annotation > tableModel : tableModels )
				tableModel.annotations();

			allTablesLoaded = true;
		}
	}
}
