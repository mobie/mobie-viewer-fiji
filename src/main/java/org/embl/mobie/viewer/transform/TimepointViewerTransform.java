package org.embl.mobie.viewer.transform;

public class TimepointViewerTransform implements ViewerTransform
{
	// Serialization
	private Integer timepoint;

	public TimepointViewerTransform( int timepoint )
	{
		this.timepoint = timepoint;
	}

	@Override
	public double[] getParameters()
	{
		return null;
	}

	@Override
	public Integer getTimepoint()
	{
		return timepoint;
	}
}
