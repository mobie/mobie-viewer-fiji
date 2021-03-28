package de.embl.cba.mobie2.segment;

import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SegmentAdapter< T extends ImageSegment >
{
	private final HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment;

	public SegmentAdapter( List< T > segments )
	{
		labelFrameAndImageToSegment = new HashMap<>();

		for ( T segment : segments )
			labelFrameAndImageToSegment.put( new LabelFrameAndImage( segment ), segment );

	}

	public T getSegment( double label, int t, String imageId )
	{
		final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( label, t, imageId  );

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
			segments.add( labelFrameAndImageToSegment.get( labelFrameAndImage ) );
		}

		return segments;
	}
}
