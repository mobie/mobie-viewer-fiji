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
import net.imglib2.util.ValuePair;
import org.embl.mobie.lib.annotation.Annotation;
import net.imglib2.util.Pair;
import org.embl.mobie.lib.io.StorageLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public interface AnnotationTableModel< A extends Annotation >
{
	default ValuePair< Double, Double > getColumnMinMax( String columnName, ArrayList< A > annotations )
	{
		double min = Double.MAX_VALUE;
		double max = -min;
		for ( A annotation : annotations )
		{
			final Double number = annotation.getNumber( columnName );

			if ( number > max )
				max = number;

			if ( number < min )
				min = number;
		}

		return new ValuePair<>( min, max );
	}

	List< String > columnNames();
	List< String > numericColumnNames();
	Class< ? > columnClass( String columnName );
	int numAnnotations(); // TODO: avoid this as it could hamper lazy loading..?! rather use rows() below? Or maybe add back the iterator?
	int rowIndexOf( A annotation );
	A annotation( int rowIndex );
	void loadTableChunk( String tableChunk ); // load additional chunks from the table source
	void loadExternalTableChunk( StorageLocation location ); // load chunks from an external table source
	//void setAvailableTableChunks( Set< String> availableColumnPaths );
	Collection< String > getAvailableTableChunks();
	LinkedHashSet< String > getLoadedTableChunks(); // loaded chunks, in the order in which they have been loaded
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
	ArrayList< A > annotations();
	void addStringColumn( String columnName );
	void addNumericColumn( String columnName );
	StorageLocation getStorageLocation();
	void transform( AffineTransform3D affineTransform3D );
	void addAnnotationListener(  AnnotationListener< A > listener );
}
