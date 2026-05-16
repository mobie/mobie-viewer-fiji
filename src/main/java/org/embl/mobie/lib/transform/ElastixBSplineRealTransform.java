package org.embl.mobie.lib.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Temporary local copy of ITC elastix-style cubic BSpline interpolation support.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class ElastixBSplineRealTransform implements RealTransform
{
	private static final int CUBIC_ORDER = 3;
	private static final int CUBIC_SAMPLES = 4;

	private final int numDimensions;
	private final List< RandomAccessibleInterval< DoubleType > > coefficients;
	private final List< RandomAccess< DoubleType > > randomAccesses;
	private final double[] gridSpacing;
	private final double[] gridOffset;
	private final int splineOrder;
	private final long[][] mins;
	private final long[][] maxs;

	public ElastixBSplineRealTransform(
			final int numDimensions,
			final List< RandomAccessibleInterval< DoubleType > > coefficients,
			final double[] gridSpacing,
			final double[] gridOffset,
			final int splineOrder )
	{
		if ( splineOrder != CUBIC_ORDER )
			throw new IllegalArgumentException( "Elastix evaluator currently supports only cubic order 3, got " + splineOrder );

		this.numDimensions = numDimensions;
		this.coefficients = new ArrayList<>( coefficients );
		this.randomAccesses = new ArrayList<>( coefficients.size() );
		this.gridSpacing = gridSpacing.clone();
		this.gridOffset = gridOffset.clone();
		this.splineOrder = splineOrder;
		this.mins = new long[ coefficients.size() ][ numDimensions ];
		this.maxs = new long[ coefficients.size() ][ numDimensions ];

		for ( int c = 0; c < coefficients.size(); c++ )
		{
			final RandomAccessibleInterval< DoubleType > coefficient = coefficients.get( c );
			randomAccesses.add( coefficient.randomAccess() );
			for ( int d = 0; d < numDimensions; d++ )
			{
				mins[ c ][ d ] = coefficient.min( d );
				maxs[ c ][ d ] = coefficient.max( d );
			}
		}
	}

	@Override
	public int numSourceDimensions()
	{
		return numDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return numDimensions;
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{
		final double[] indexCoordinates = new double[ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
			indexCoordinates[ d ] = ( source[ d ] - gridOffset[ d ] ) / gridSpacing[ d ];

		for ( int c = 0; c < numDimensions; c++ )
			target[ c ] = source[ c ] + evaluateComponent( c, indexCoordinates );
	}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		final double[] sourceArray = new double[ numDimensions ];
		source.localize( sourceArray );
		final double[] targetArray = new double[ numDimensions ];
		apply( sourceArray, targetArray );
		target.setPosition( targetArray );
	}

	private double evaluateComponent( final int component, final double[] position )
	{
		final int[] base = new int[ numDimensions ];
		final double[][] weights = new double[ numDimensions ][ CUBIC_SAMPLES ];
		for ( int d = 0; d < numDimensions; d++ )
		{
			base[ d ] = ( int ) Math.floor( position[ d ] ) - 1;
			weights[ d ] = cubicWeights( position[ d ] - Math.floor( position[ d ] ) );
		}

		final long[] coordinate = new long[ numDimensions ];
		return accumulate( component, 0, 1.0, coordinate, base, weights );
	}

	private double accumulate(
			final int component,
			final int dimension,
			final double currentWeight,
			final long[] coordinate,
			final int[] base,
			final double[][] weights )
	{
		if ( dimension == numDimensions )
		{
			final RandomAccess< DoubleType > access = randomAccesses.get( component );
			for ( int d = 0; d < numDimensions; d++ )
				access.setPosition( coordinate[ d ], d );
			return currentWeight * access.get().getRealDouble();
		}

		double sum = 0.0;
		for ( int k = 0; k < CUBIC_SAMPLES; k++ )
		{
			coordinate[ dimension ] = clamp( base[ dimension ] + k, mins[ component ][ dimension ], maxs[ component ][ dimension ] );
			sum += accumulate( component, dimension + 1, currentWeight * weights[ dimension ][ k ], coordinate, base, weights );
		}
		return sum;
	}

	private static long clamp( final long value, final long min, final long max )
	{
		if ( value < min )
			return min;
		if ( value > max )
			return max;
		return value;
	}

	private static double[] cubicWeights( final double t )
	{
		final double t2 = t * t;
		final double t3 = t2 * t;
		return new double[] {
				Math.pow( 1.0 - t, 3.0 ) / 6.0,
				( 3.0 * t3 - 6.0 * t2 + 4.0 ) / 6.0,
				( -3.0 * t3 + 3.0 * t2 + 3.0 * t + 1.0 ) / 6.0,
				t3 / 6.0
		};
	}

	@Override
	public RealTransform copy()
	{
		return new ElastixBSplineRealTransform(
				numDimensions,
				coefficients,
				Arrays.copyOf( gridSpacing, gridSpacing.length ),
				Arrays.copyOf( gridOffset, gridOffset.length ),
				splineOrder
		);
	}
}

