package org.embl.mobie.viewer.segment;

import de.embl.cba.tables.imagesegment.DefaultImageSegment;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SegmentAdapter< T extends ImageSegment >
{
	private HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment;

	/**
	 * For lazy initialization
	 */
	public SegmentAdapter()
	{
		labelFrameAndImageToSegment = new HashMap<>();
	}

	public SegmentAdapter( List< T > segments )
	{
		labelFrameAndImageToSegment = new HashMap<>();

		for ( T segment : segments )
			labelFrameAndImageToSegment.put( new LabelFrameAndImage( segment ), segment );
	}

	public T getSegment( double label, int t, String imageId )
	{
		final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( label, t, imageId  );

		return getSegment( labelFrameAndImage );
	}

	private T getSegment( LabelFrameAndImage labelFrameAndImage )
	{
		if ( ! labelFrameAndImageToSegment.containsKey( labelFrameAndImage ) )
		{
			final DefaultImageSegment defaultImageSegment = new DefaultImageSegment( labelFrameAndImage.getImage(), labelFrameAndImage.getLabel(), labelFrameAndImage.getFrame(), 0, 0, 0, null );
			labelFrameAndImageToSegment.put( labelFrameAndImage, ( T ) defaultImageSegment );
		}

		return labelFrameAndImageToSegment.get( labelFrameAndImage );
	}

	// deserialize
	public List< T > getSegments( List< String > serialisedSegments )
	{
		final ArrayList< T > segments = new ArrayList<>();
		for ( String serialisedSegment : serialisedSegments )
		{
			final String[] split = serialisedSegment.split( ";" );
			final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( Double.parseDouble( split[2] ), Integer.parseInt( split[1] ), split[0] );
			segments.add( getSegment( labelFrameAndImage ) );
		}

		return segments;
	}
}
