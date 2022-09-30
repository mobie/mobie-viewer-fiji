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

import org.embl.mobie.viewer.table.AnnData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationAdapter< A extends Annotation >
{
	private final AnnData< A > annData;
	private Map< String, A > uuidToAnnotation;
	private Map< String, A > itlToAnnotation; // source, timepoint, label

	public AnnotationAdapter( AnnData< A > annData )
	{
		this.annData = annData;
	}

	public A createVariable()
	{
		// MAY
		//  is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().annotation( 0 );
	}

	// UUID for deserialisation of selected segments
	// https://github.com/mobie/mobie-viewer-fiji/issues/827
	public A getAnnotation( String uuid )
	{
		if ( uuidToAnnotation == null )
			initMaps();

		return uuidToAnnotation.get( uuid );
	}

	// This is for mapping from within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	public A getAnnotation( String source, int timePoint, int label )
	{
		if ( itlToAnnotation == null )
			initMaps();
		final String itl = stlKey( source, timePoint, label );
		return itlToAnnotation.get( itl );
	}

	private synchronized void initMaps()
	{
		uuidToAnnotation = new HashMap<>();
		itlToAnnotation = new HashMap<>();
		final Iterator< A > iterator = annData.getTable().annotations().iterator();
		while( iterator.hasNext() )
		{
			A annotation = iterator.next();
			uuidToAnnotation.put( annotation.uuid(), annotation );
			itlToAnnotation.put( stlKey( annotation.source(), annotation.timePoint(), annotation.label() ), annotation );
		}
	}

	private String stlKey( String source, int timePoint, int label )
	{
		return source + ";" + timePoint + ";" + label;
	}

	public Set< A > getAnnotations( Set< String > annotationIds )
	{
		return annotationIds.stream().map( id -> getAnnotation( id ) ).collect( Collectors.toSet() );
	}
}
