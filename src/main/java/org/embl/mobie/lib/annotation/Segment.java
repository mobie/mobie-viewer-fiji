package org.embl.mobie.lib.annotation;

import net.imglib2.RealInterval;

public interface Segment extends Location
{
	String imageId();
	int label();

	RealInterval boundingBox();
	void setBoundingBox( RealInterval boundingBox );

	float[] mesh();
	void setMesh( float[] mesh );
}
