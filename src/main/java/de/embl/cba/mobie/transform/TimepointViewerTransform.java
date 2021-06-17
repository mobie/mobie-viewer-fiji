package de.embl.cba.mobie.transform;

public class TimepointViewerTransform implements ViewerTransform
{
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
