package de.embl.cba.mobie.transform;

public class NormalizedAffineViewerTransform implements ViewerTransform
{
	private double[] normalizedAffine;
	private Integer timepoint;

	public NormalizedAffineViewerTransform( double[] parameters, int timepoint )
	{
		this.normalizedAffine = parameters;
		this.timepoint = timepoint;
	}

	public double[] getParameters()
	{
		return normalizedAffine;
	}

	@Override
	public Integer getTimepoint()
	{
		return timepoint;
	}
}
