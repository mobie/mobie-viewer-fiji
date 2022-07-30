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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// MAYBE AnnData or AnnotationTableModel could do that itself?!

public class AnnotationProvider< A extends Annotation > implements AnnotationProviderInterface< A >
{
	private final AnnData< A > annData;
	private Map< String, A > idToAnnotation;

	public AnnotationProvider( AnnData< A > annData )
	{
		this.annData = annData;
	}

	@Override
	public A getAnnotation( String annotationId )
	{
		if ( idToAnnotation == null )
			initSegmentMap();

		return idToAnnotation.get( annotationId  );
	}

	@Override
	public A createVariable()
	{
		// MAY
		//  is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().row( 0 );
	}

	private synchronized void initSegmentMap()
	{
		idToAnnotation = new ConcurrentHashMap<>();
		final Iterator< A > iterator = annData.getTable().rows().iterator();
		while( iterator.hasNext() )
		{
			A annotation = iterator.next();
			idToAnnotation.put( annotation.getId(), annotation );
		}
	}

}
