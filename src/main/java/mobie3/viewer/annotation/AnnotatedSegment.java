package mobie3.viewer.annotation;

public interface AnnotatedSegment extends Segment, Annotation
{
	static String toAnnotationId( String imageId, int timePoint, int labelId )
	{
		return ""+imageId+";"+timePoint+";"+labelId;
	}

	static String toAnnotationId( Segment segment )
	{
		return toAnnotationId( segment.imageId(), segment.timePoint(), segment.labelId() );
	}
}
