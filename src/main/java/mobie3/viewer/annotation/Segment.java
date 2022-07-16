package mobie3.viewer.annotation;

import net.imglib2.RealInterval;

public interface Segment extends Location
{
	String imageId();
	int labelId();

	// create ID (also used for serialisation)
	static String toAnnotationId( String imageId, int timePoint, int labelId )
	{
		return ""+imageId+";"+timePoint+";"+labelId;
	}

	// create ID (also used for serialisation)
	static String toAnnotationId( Segment segment )
	{
		return toAnnotationId( segment.imageId(), segment.timePoint(), segment.labelId() );
	}

	RealInterval boundingBox();
	void setBoundingBox( RealInterval boundingBox );

	float[] mesh();
	void setMesh( float[] mesh );
}
