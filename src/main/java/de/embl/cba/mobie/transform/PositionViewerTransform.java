package de.embl.cba.mobie.transform;

public class PositionViewerTransform implements ViewerTransform
{
	private double[] position;

	public PositionViewerTransform( double[] parameters )
	{
		this.position = parameters;
	}

	public double[] getParameters()
	{
		return position;
	}
}
