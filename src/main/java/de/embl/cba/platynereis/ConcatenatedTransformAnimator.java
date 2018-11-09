package de.embl.cba.platynereis;

import bdv.viewer.animate.AbstractTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;

public class ConcatenatedTransformAnimator extends AbstractTransformAnimator
{

	private final List< ? extends AbstractTransformAnimator > list;

	public ConcatenatedTransformAnimator( long duration, List< ? extends AbstractTransformAnimator> list)
	{
		super( duration );
		this.list = list;
	}

	@Override
	public AffineTransform3D get( double t )
	{
		int n = list.size();
		double s = t * n;
		int step = s == n ? n - 1 : (int) s;
		double stepT = s - step;
		return list.get( step ).get(stepT);
	}

}
