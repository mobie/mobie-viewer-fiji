package org.embl.mobie.lib.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Temporary local copy of ITC elastix BSpline converter support.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class ElastixBSplineToBSplineRealTransform
{
	public static RealTransform convert( final ElastixBSplineTransform elastixBSplineTransform )
	{
		final int nd = elastixBSplineTransform.FixedImageDimension;
		if ( nd != 3 )
			throw new IllegalArgumentException( "Only 3D Elastix BSpline transforms are currently supported, got " + nd + "D" );
		return new ElastixBSplineRealTransform(
				nd,
				coefficients( elastixBSplineTransform ),
				spacing( elastixBSplineTransform ),
				origin( elastixBSplineTransform ),
				splineOrder( elastixBSplineTransform )
		);
	}

	private static List< RandomAccessibleInterval< DoubleType > > coefficients( final ElastixBSplineTransform elastixBSplineTransform )
	{
		final int nd = elastixBSplineTransform.FixedImageDimension;
		final ArrayList< RandomAccessibleInterval< DoubleType > > coefficients = new ArrayList<>( nd );
		for ( int i = 0; i < nd; i++ )
			coefficients.add( elastixBSplineTransform.getBSplineCoefficients( i ) );
		return coefficients;
	}

	private static double[] spacing( final ElastixBSplineTransform elastixBSplineTransform )
	{
		return Arrays.stream( elastixBSplineTransform.GridSpacing ).mapToDouble( x -> x ).toArray();
	}

	private static double[] origin( final ElastixBSplineTransform elastixBSplineTransform )
	{
		return Arrays.stream( elastixBSplineTransform.GridOrigin ).mapToDouble( x -> x ).toArray();
	}

	private static int splineOrder( final ElastixBSplineTransform elastixBSplineTransform )
	{
		return elastixBSplineTransform.BSplineTransformSplineOrder == null ? 3 : elastixBSplineTransform.BSplineTransformSplineOrder;
	}
}

