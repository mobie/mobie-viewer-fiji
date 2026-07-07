/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
import org.embl.mobie.lib.transform.AnnotationTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class TransformedAnnotationTableModel< A extends Annotation, TA extends A > extends AbstractAnnotationTableModel< TA >
{
	private final AnnotationTableModel< A > wrappedTableModel;
	private final AnnotationTransformer< A, TA > transformer;
	private ArrayList< TA > annotations;

	public TransformedAnnotationTableModel( AnnotationTableModel< A > tableModel, AnnotationTransformer< A, TA > transformer )
	{
		this.wrappedTableModel = tableModel;
		this.transformer = transformer;
	}

	@Override
	public List< String > columnNames()
	{
		return wrappedTableModel.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return wrappedTableModel.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return wrappedTableModel.columnClass( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return wrappedTableModel.numAnnotations();
	}

	@Override
	public int rowIndexOf( TA annotation )
	{
		initTransformedAnnotations();
		return annotations.indexOf( annotation );
	}

	@Override
	public TA annotation( int rowIndex )
	{
		initTransformedAnnotations();
		return annotations.get( rowIndex );
	}

	@Override
	public void loadTableChunk( String tableChunk )
	{
		wrappedTableModel.loadTableChunk( tableChunk );
	}

	@Override
	public void loadExternalTableChunk( StorageLocation location )
	{
		wrappedTableModel.loadExternalTableChunk( location );
	}


	@Override
	public Collection< String > getAvailableTableChunks()
	{
		return wrappedTableModel.getAvailableTableChunks();
	}

	@Override
	public LinkedHashSet< String > getLoadedTableChunks()
	{
		return wrappedTableModel.getLoadedTableChunks();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public ArrayList< TA > annotations()
	{
		initTransformedAnnotations();

		return annotations;
	}

	private synchronized void initTransformedAnnotations()
	{
		if ( annotations == null )
		{
			annotations = new ArrayList<>();

			final int numAnnotations = wrappedTableModel.numAnnotations();
			for ( int rowIndex = 0; rowIndex < numAnnotations; rowIndex++ )
			{
				final TA transformedAnnotation = transformer.transform( wrappedTableModel.annotation( rowIndex ) );
				annotations.add( transformedAnnotation );
			}

			for ( AnnotationListener< TA > listener : listeners.list )
				listener.annotationsAdded( annotations );
		}
	}

	@Override
	public void addStringColumn( String columnName )
	{
		if ( columnNames().contains( columnName ) )
			return;

		wrappedTableModel.addStringColumn( columnName );

		for ( AnnotationListener< TA > listener : listeners.list )
			listener.columnsAdded( null );
	}

	@Override
	public void addNumericColumn( String columnName )
	{
		if ( columnNames().contains( columnName ) )
			return;

		wrappedTableModel.addNumericColumn( columnName );

		for ( AnnotationListener< TA > listener : listeners.list )
			listener.columnsAdded( null );
	}

	@Override
	public StorageLocation getStorageLocation()
	{
		return wrappedTableModel.getStorageLocation();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		throw new RuntimeException("Transforming a TransformedAnnotationTableModel is not yet implemented.");
	}

	@Override
	public void addAnnotationListener( AnnotationListener< TA > listener )
	{
		listeners.add( listener );
	}

}
