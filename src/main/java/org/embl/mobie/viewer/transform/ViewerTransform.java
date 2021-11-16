package org.embl.mobie.viewer.transform;

public interface ViewerTransform
{
	double[] getParameters();

	Integer getTimepoint(); // May return NULL in the implementation
}
