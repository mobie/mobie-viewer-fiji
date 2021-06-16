package de.embl.cba.mobie.transform;

public class NormalizedAffineViewerTransform implements ViewerTransform
{
	private double[] normalizedAffine;

	public NormalizedAffineViewerTransform( double[] parameters )
	{
		this.normalizedAffine = parameters;
	}

	public double[] getParameters()
	{
		return normalizedAffine;
	}
}
