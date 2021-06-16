package de.embl.cba.mobie.transform;

public class AffineViewerTransform implements ViewerTransform
{
	private double[] affineTransform;

	public AffineViewerTransform( double[] parameters )
	{
		this.affineTransform = parameters;
	}

	public double[] getParameters()
	{
		return affineTransform;
	}
}
