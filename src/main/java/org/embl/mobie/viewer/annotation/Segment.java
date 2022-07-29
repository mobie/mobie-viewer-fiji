package org.embl.mobie.viewer.annotation;

import net.imglib2.RealInterval;

public interface Segment extends Location
{
	String imageId();
	int labelId();

	RealInterval boundingBox();
	void setBoundingBox( RealInterval boundingBox );

	float[] mesh();
	void setMesh( float[] mesh );
}
