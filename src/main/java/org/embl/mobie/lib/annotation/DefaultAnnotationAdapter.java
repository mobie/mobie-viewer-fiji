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

import org.embl.mobie.lib.table.AnnData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultAnnotationAdapter< A extends Annotation > implements AnnotationAdapter< A >
{
	private final AtomicBoolean throwError = new AtomicBoolean( true );
	private final AnnData< A > annData;
	private final String source;
	private final A annotation;
	private Map< String, A > stlToAnnotation; // source, timepoint, label

	public DefaultAnnotationAdapter( AnnData< A > annData )
	{
		this.annData = annData;
		this.source = null;
		this.annotation = null;
	}

	public DefaultAnnotationAdapter( AnnData< A > annData, String source )
	{
		this.annData = annData;
		this.source = source;
		this.annotation = null;
	}

	public DefaultAnnotationAdapter( AnnData< A > annData, String source, A annotation )
	{
		this.annData = annData;
		this.source = source;
		this.annotation = annotation;
	}

	// FIXME: Can we get rid of this? Currently not used...
	@Override
	public A createVariable()
	{
		return annData.getTable().annotation( 0 );
	}

	// This is for mapping for voxels within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	@Override
	public synchronized A getAnnotation( String source, final int timePoint, final int label )
	{
		if ( label == 0 )
		{
			// 0 is the background label
			// null is the background annotation
			return null ;
		}

		if ( this.source != null )
		{
			// this is needed, e.g., when an image is transformed and
			// has a different name than the wrapped original image
			source = this.source;
		}

		final String stl = stlKey( source, timePoint, label );
		final A annotation = stlToAnnotation.get( stl );

		if ( annotation == null )
		{
			// FIXME: Check whether this could be done lazy?
			if ( throwError.get() )
			{
				System.err.println( "AnnotationAdapter: Missing annotation: " + source+ "; time point = " + timePoint + "; label = " + label + "\nSuppressing further errors of that kind." );
				System.err.println( "AnnotationAdapter: Suppressing further errors of that kind.");
			}

			throwError.set( false );
		}

		return annotation;
	}

	@Override
	public void init()
	{
		stlToAnnotation = new ConcurrentHashMap<>();
		final Iterator< A > iterator = annData.getTable().annotations().iterator();
		while( iterator.hasNext() )
		{
			A annotation = iterator.next();
			stlToAnnotation.put( stlKey( annotation.source(), annotation.timePoint(), annotation.label() ), annotation );
		}
	}

	private String stlKey( String source, int timePoint, int label )
	{
		return source + ";" + timePoint + ";" + label;
	}
}
