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
package org.embl.mobie.lib.annotation;

import org.embl.mobie.lib.table.DefaultAnnotatedSegment;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentAdapter implements AnnotationAdapter< AnnotatedSegment >, LazyAnnotationAdapter< AnnotatedSegment >
{
	private final String name;
	private final LazyAnnotatedSegmentTableModel tableModel;
	private Map< String, AnnotatedSegment > stlToAnnotation; // source, timepoint, label

	public LazyAnnotatedSegmentAdapter( String name, LazyAnnotatedSegmentTableModel tableModel )
	{
		this.name = name;
		this.tableModel = tableModel;
		this.stlToAnnotation = new ConcurrentHashMap<>();
	}

	@Override
	public AnnotatedSegment createVariable()
	{
		return new DefaultAnnotatedSegment( name, 0, 0 );
	}

	// This is for mapping for voxels within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	@Override
	public AnnotatedSegment getAnnotation( String source, int timePoint, int label )
	{
		if ( label == 0 )
		{
			// 0 is the background label
			// null is the background annotation
			return null ;
		}

		final String stl = stlKey( source, timePoint, label );

		if ( ! stlToAnnotation.containsKey( stl ) )
		{
			synchronized ( stlToAnnotation )
			{
				if ( stlToAnnotation.containsKey( stl ) )
					return stlToAnnotation.get( stl );

				final AnnotatedSegment annotatedSegment = tableModel.createAnnotation( source, timePoint, label );
				stlToAnnotation.put( stl, annotatedSegment );
			}
		}

		return stlToAnnotation.get( stl );
	}

	@Override
	public void init()
	{

	}

	private String stlKey( String source, int timePoint, int label )
	{
		return source + ";" + timePoint + ";" + label;
	}
}
