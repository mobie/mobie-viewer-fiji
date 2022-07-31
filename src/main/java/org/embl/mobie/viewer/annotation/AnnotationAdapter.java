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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AnnotationAdapter< A extends Annotation >
{
	private final AnnData< A > annData;
	private Map< String, A > timePointAndLabelToAnnotation;
	private Map< String, A > annotationIdToAnnotation;

	public AnnotationAdapter( AnnData< A > annData )
	{
		this.annData = annData;
	}

	public A createVariable()
	{
		// MAY
		//  is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().row( 0 );
	}

	public A getAnnotation( String id )
	{
		if ( annotationIdToAnnotation == null )
			initMaps();

		return annotationIdToAnnotation.get( id );
	}

	public A getAnnotation( int timePoint, int label )
	{
		if ( timePointAndLabelToAnnotation == null )
			initMaps();

		return timePointAndLabelToAnnotation.get( getKey( timePoint, label ));
	}

	private synchronized void initMaps()
	{
		timePointAndLabelToAnnotation = new HashMap<>();
		annotationIdToAnnotation = new HashMap<>();
		final Iterator< A > iterator = annData.getTable().rows().iterator();
		while( iterator.hasNext() )
		{
			A annotation = iterator.next();
			timePointAndLabelToAnnotation.put( getKey( annotation.timePoint(), annotation.label() ), annotation );
			annotationIdToAnnotation.put( annotation.id(), annotation );
		}
	}

	private String getKey( int timePoint, int label )
	{
		return timePoint + "_" + label;
	}

	public Set< A > getAnnotations( Set< String > annotationIds )
	{
		return annotationIds.stream().map( id -> getAnnotation( id ) ).collect( Collectors.toSet() );
	}
}
