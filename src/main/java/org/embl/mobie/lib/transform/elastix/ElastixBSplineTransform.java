package org.embl.mobie.lib.transform.elastix;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

/**
 * Temporary local copy of ITC BSpline transform metadata container.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class ElastixBSplineTransform extends ElastixTransform
{
	public Integer[] GridSize;
	public Integer[] GridIndex;
	public Double[] GridSpacing;
	public Double[] GridOrigin;
	public Double[] GridDirection;
	public Integer BSplineTransformSplineOrder;
	public Boolean UseCyclicTransform;

	public RandomAccessibleInterval< DoubleType > getBSplineCoefficients( final int coordinate )
	{
		final long nCoefficients = coefficientCountPerDimension();
		if ( NumberOfParameters == null || TransformParameters == null )
			throw new IllegalStateException( "Missing TransformParameters / NumberOfParameters" );
		if ( nCoefficients * GridSize.length != NumberOfParameters )
			throw new IllegalStateException( "Unexpected coefficient count for GridSize" );

		final long[] gridDimensions = Arrays.stream( GridSize ).mapToLong( x -> x ).toArray();
		final FinalInterval gridInterval = new FinalInterval( gridDimensions );
		final Img< DoubleType > coefficients = Util.getSuitableImgFactory( gridInterval, new DoubleType() ).create( gridInterval );
		final Cursor< DoubleType > cursor = coefficients.cursor();

		int parameterIndex = ( int ) ( coordinate * nCoefficients );
		while ( cursor.hasNext() )
			cursor.next().set( TransformParameters[ parameterIndex++ ] );

		return coefficients;
	}

	public long coefficientCountPerDimension()
	{
		long n = 1;
		for ( int d = 0; d < GridSize.length; d++ )
			n *= GridSize[ d ];
		return n;
	}
}

