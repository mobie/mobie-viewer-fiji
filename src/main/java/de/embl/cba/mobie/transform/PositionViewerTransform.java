package de.embl.cba.mobie.transform;

public class PositionViewerTransform implements ViewerTransform
{
	private double[] position;
	private Integer timepoint;

	public PositionViewerTransform( double[] parameters, int timepoint )
	{
		this.position = parameters;
		this.timepoint = timepoint;
	}

	public double[] getParameters()
	{
		return position;
	}

	@Override
	public Integer getTimepoint()
	{
		return timepoint;
	}
}
