package de.embl.cba.mobie.transform;

public class AffineViewerTransform implements ViewerTransform
{
	private double[] affineTransform;
	private final Integer timepoint;

	public AffineViewerTransform( double[] parameters, int timepoint )
	{
		this.affineTransform = parameters;
		this.timepoint = timepoint;
	}

	@Override
	public double[] getParameters()
	{
		return affineTransform;
	}

	@Override
	public Integer getTimepoint()
	{
		return timepoint;
	}
}
