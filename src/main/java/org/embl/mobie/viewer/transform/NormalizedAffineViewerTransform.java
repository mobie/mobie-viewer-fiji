package org.embl.mobie.viewer.transform;

public class NormalizedAffineViewerTransform implements ViewerTransform
{
	// Serialization
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
