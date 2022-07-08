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

import de.embl.cba.tables.imagesegment.DefaultImageSegment;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SegmentAdapter< S extends ImageSegment >
{
	private List< S > segments;
	private Map< LabelFrameAndImage, S > labelFrameAndImageToSegment;
	private boolean isLazy = false;

	/**
	 * For lazy initialization
	 */
	public SegmentAdapter()
	{
		this( null );
	}

	public SegmentAdapter( @Nullable List< S > segments )
	{
		labelFrameAndImageToSegment = new ConcurrentHashMap<>();

		if ( segments == null )
		{
			isLazy = true;
		}
		else
		{
			this.segments = segments;
			for ( S segment : segments )
				labelFrameAndImageToSegment.put(
						new LabelFrameAndImage( segment ), segment );
		}
	}

	public synchronized boolean containsSegment( double label, int t, String imageId )
	{
		final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( label, t, imageId  );

		return labelFrameAndImageToSegment.containsKey( labelFrameAndImage );
	}

	public synchronized S getSegmentCreateIfNotExist( double label, int t, String imageId )
	{
		final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( label, t, imageId  );

		if ( ! labelFrameAndImageToSegment.containsKey( labelFrameAndImage ) )
		{
			final DefaultImageSegment defaultImageSegment = new DefaultImageSegment( labelFrameAndImage.getImage(), labelFrameAndImage.getLabel(), labelFrameAndImage.getFrame(), 0, 0, 0, null );
			labelFrameAndImageToSegment.put( labelFrameAndImage, ( S ) defaultImageSegment );
		}

		return labelFrameAndImageToSegment.get( labelFrameAndImage );
	}

	public S createVariable()
	{
		return segments.get( 0 );
	}

	public synchronized S getSegment( double label, int t, String imageId )
	{
		if ( isLazy )
		{
			return getSegmentCreateIfNotExist( label, t, imageId );
		}
		else
		{
			final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( label, t, imageId );
			return getSegment( labelFrameAndImage );
		}
	}

	public synchronized S getSegment( LabelFrameAndImage labelFrameAndImage )
	{
		if ( segments.size() > labelFrameAndImageToSegment.size() )
		{
			// segments have been added (lazy loaded)
			// thus we need to update the map
			final int currentSize = labelFrameAndImageToSegment.size();
			final int newSize = segments.size();
			for ( int i = currentSize; i < newSize; i++ )
			{
				final S imageSegment = segments.get( i );
				labelFrameAndImageToSegment.put( new LabelFrameAndImage( imageSegment ), imageSegment );
			}
		}

		return labelFrameAndImageToSegment.get( labelFrameAndImage  );
	}

	// deserialize
	public List< S > getSegments( List< String > serialisedSegments )
	{
		final ArrayList< S > segments = new ArrayList<>();
		for ( String serialisedSegment : serialisedSegments )
		{
			final String[] split = serialisedSegment.split( ";" );
			final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( Double.parseDouble( split[2] ), Integer.parseInt( split[1] ), split[0] );
			segments.add( getSegment( labelFrameAndImage ) );
		}

		return segments;
	}
}
