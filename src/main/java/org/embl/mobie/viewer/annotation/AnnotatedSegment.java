package org.embl.mobie.viewer.annotation;

public interface AnnotatedSegment extends Segment, Annotation
{
	static String createId( String imageId, int timePoint, int labelId )
	{
		return ""+imageId+";"+timePoint+";"+labelId;
	}

	static String createId( Segment segment )
	{
		return createId( segment.imageId(), segment.timePoint(), segment.label() );
	}
}
