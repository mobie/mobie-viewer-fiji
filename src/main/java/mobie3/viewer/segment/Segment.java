package mobie3.viewer.segment;

import net.imglib2.RealInterval;

public interface Segment
{
	// Properties that MUST be contained
	// in a corresponding table row.
	String imageId();
	int labelId();
	int timePoint();
	double[] getAnchor();

	// Properties that MAY be contained
	// in a corresponding table row.
	RealInterval boundingBox();
	void setBoundingBox( RealInterval boundingBox );

	// Properties that are computed
	// on the fly, when needed.
	float[] mesh();
	void setMesh( float[] mesh );
}
