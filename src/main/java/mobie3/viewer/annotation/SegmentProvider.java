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
package mobie3.viewer.annotation;

import mobie3.viewer.table.AnnData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SegmentProvider< AS extends AnnotatedSegment > implements AnnotationProvider< AS >
{
	private final AnnData< AS > annData;
	private Map< String, AS > segmentMap;

	public SegmentProvider( AnnData< AS > annData )
	{
		this.annData = annData;
	}

	@Override
	public AS getAnnotation( String annotationId )
	{
		if ( segmentMap == null )
			initSegmentMap();

		return segmentMap.get( annotationId  );
	}

	@Override
	public AS createVariable()
	{
		// TODO: is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().row( 0 );
	}

	private synchronized void initSegmentMap()
	{
		segmentMap = new ConcurrentHashMap<>();

		final Iterator< AS > iterator = annData.getTable().rows().iterator();
		while( iterator.hasNext() )
		{
			AS segment = iterator.next();
			segmentMap.put( AnnotatedSegment.createId( segment ), segment );
		}
	}

}
