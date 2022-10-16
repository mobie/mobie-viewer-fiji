/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.annotation;

import org.embl.mobie.viewer.table.DefaultAnnotatedSegment;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentAdapter implements AnnotationAdapter< AnnotatedSegment >
{
	private Map< String, AnnotatedSegment > uuidToAnnotation;
	private Map< String, AnnotatedSegment > stlToAnnotation; // source, timepoint, label

	public LazyAnnotatedSegmentAdapter( )
	{
		uuidToAnnotation = new ConcurrentHashMap<>();
		stlToAnnotation = new ConcurrentHashMap<>();
	}

	@Override
	public AnnotatedSegment createVariable()
	{
		return new DefaultAnnotatedSegment( 0 );
	}

	// UUID for de-serialisation of selected segments
	// https://github.com/mobie/mobie-viewer-fiji/issues/827
	@Override
	public AnnotatedSegment getAnnotation( String uuid )
	{
		throw new UnsupportedOperationException("Fetching annotations via UUID for segmentation images without a table is not yet implemented.");
	}

	// This is for mapping for voxels within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	@Override
	public synchronized AnnotatedSegment getAnnotation( String source, int timePoint, int label )
	{
		if ( label == 0 )
		{
			// 0 is the background label
			// null is the background annotation
			return null ;
		}

		// FIXME The fact that the method is synchronized makes
		//   rendering in BDV effectively single threaded!
		//   Once itlToAnnotation is initialised this does
		//   not need to be synchronised anymore.
		final String itl = stlKey( source, timePoint, label );
		if ( ! stlToAnnotation.containsKey( itl ) )
		{
			// FIXME lazy create the annotation
			//   in the table model, or just here?
			//   https://github.com/mobie/mobie-viewer-fiji/issues/862
			//   maybe initially more work, but long term easier
			//   to create it in the table, because then all the downstream
			//   code can rely on the table being present and
			//   I don't have to implement special code for the case without
			//   tables.
		}
		final AnnotatedSegment annotation = stlToAnnotation.get( itl );
		return annotation;
	}

	private String stlKey( String source, int timePoint, int label )
	{
		return source + ";" + timePoint + ";" + label;
	}

	@Override
	public Set< AnnotatedSegment > getAnnotations( Set< String > uuids )
	{
		return uuids.stream().map( uuid -> getAnnotation( uuid ) ).collect( Collectors.toSet() );
	}
}
