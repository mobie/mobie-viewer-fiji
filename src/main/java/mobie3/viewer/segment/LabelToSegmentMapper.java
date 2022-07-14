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
package mobie3.viewer.segment;

import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.AnnotatedSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class LabelToSegmentMapper< AS extends AnnotatedSegment > implements SegmentProvider< AS >
{
	private final AnnData< AS > annData;
	private Map< Label, AS > labelToSegment;

	public LabelToSegmentMapper( AnnData< AS > annData )
	{
		this.annData = annData;
	}

	public AS getSegment( int labelId, int t, String imageId )
	{
		final Label label = new Label( labelId, t, imageId );

		if ( labelToSegment == null )
		{
			initMapping();
		}

		return labelToSegment.get( label  );
	}

	@Override
	public AS createVariable()
	{
		// TODO: is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().row( 0 );
	}

	private synchronized void initMapping()
	{
		labelToSegment = new ConcurrentHashMap<>();
		final int numRows = annData.getTable().numRows();
		for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
		{
			AS segment = annData.getTable().row( rowIndex );
			final Label label = new Label( segment.labelId(), segment.timePoint(), segment.imageId() );
			labelToSegment.put( label, segment );
		}
	}

	public List< AS > getSegments( List< String > serialisedSegments )
	{
		final ArrayList< AS > segments = new ArrayList<>();
		for ( String serialisedSegment : serialisedSegments )
		{
			final String[] split = serialisedSegment.split( ";" );
			final int labelId = Integer.parseInt( split[ 2 ] );
			final int timePoint = Integer.parseInt( split[ 1 ] );
			final String imageId = split[ 0 ];
			segments.add( getSegment( labelId, timePoint, imageId ) );
		}
		return segments;
	}
}
