package org.embl.mobie.lib.transform;

import java.util.Arrays;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.RealTransform;

/**
 * Temporary local copy of ITC iterative inverse BSpline support.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class InverseElastixBSplineRealTransform implements RealTransform
{
	private static final int DEFAULT_MAX_ITERATIONS = 80;
	private static final double DEFAULT_TOLERANCE = 1.0; // 1e-6;
	private static final double DEFAULT_DAMPING = 1.0;

	private final RealTransform forward;
	private final int numDimensions;
	private final int maxIterations;
	private final double tolerance;
	private final double damping;

	public InverseElastixBSplineRealTransform( final RealTransform forward )
	{
		this( forward, DEFAULT_MAX_ITERATIONS, DEFAULT_TOLERANCE, DEFAULT_DAMPING );
	}

	public InverseElastixBSplineRealTransform(
			final RealTransform forward,
			final int maxIterations,
			final double tolerance,
			final double damping )
	{
		if ( forward == null )
			throw new IllegalArgumentException( "forward transform cannot be null" );
		if ( maxIterations <= 0 )
			throw new IllegalArgumentException( "maxIterations must be > 0" );
		if ( tolerance <= 0 )
			throw new IllegalArgumentException( "tolerance must be > 0" );
		if ( damping <= 0 || damping > 1.0 )
			throw new IllegalArgumentException( "damping must be in (0, 1]" );
		if ( forward.numSourceDimensions() != forward.numTargetDimensions() )
			throw new IllegalArgumentException( "forward transform must have equal source/target dimensions" );

		this.forward = forward;
		this.numDimensions = forward.numSourceDimensions();
		this.maxIterations = maxIterations;
		this.tolerance = tolerance;
		this.damping = damping;
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
		// For the inverse we need to find the target point such that f(target) = source

		final double[] x = Arrays.copyOf( source, numDimensions );
		final double[] fx = new double[ numDimensions ];

		for ( int iteration = 0; iteration < maxIterations; iteration++ )
		{
			forward.apply( x, fx );

			double maxResidual = 0.0;
			for ( int d = 0; d < numDimensions; d++ )
			{
				final double residual = fx[ d ] - source[ d ];
				maxResidual = Math.max( maxResidual, Math.abs( residual ) );
				x[ d ] -= damping * residual;
			}

			if ( maxResidual < tolerance )
				break;

			if ( iteration == maxIterations - 1 )
			{
				int a = 1;
			}
		}



		System.arraycopy( x, 0, target, 0, numDimensions );
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

	@Override
	public RealTransform copy()
	{
		return new InverseElastixBSplineRealTransform( forward.copy(), maxIterations, tolerance, damping );
	}
}

