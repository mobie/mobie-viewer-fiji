package org.embl.mobie.lib.transform;

import ij.IJ;
import net.imglib2.realtransform.RealTransform;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;
import org.embl.mobie.lib.transform.elastix.ElastixTransform;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RealTransformProvider
{
	private static final int INVERSE_SAMPLING_FACTOR = 1;
	private static final int INVERSE_MAX_ITERATIONS = 80;
	private static final double INVERSE_TOLERANCE = 1e-6;
	private static final double INVERSE_DAMPING = 1.0;

	private final Map< String, RealTransform > elastixBsplineCache = new ConcurrentHashMap<>();
	private final Map< String, RealTransform > displacementFieldCache = new ConcurrentHashMap<>();

	public RealTransform getDisplacementFieldRealTransform( final String displacementFieldUri ) throws Exception
	{
		final RealTransform cached = displacementFieldCache.get( displacementFieldUri );
		if ( cached != null )
			return cached;

		final RealTransform transform = DisplacementFieldTransformIO.load( displacementFieldUri );
		displacementFieldCache.put( displacementFieldUri, transform );
		return transform;
	}

	public RealTransform getElastixBSplineRealTransform( final String parametersUri, boolean inverse ) throws Exception
	{
		final String cacheKey = parametersUri
				+ "|inverse=" + inverse
				+ "|samplingFactor=" + INVERSE_SAMPLING_FACTOR
				+ "|maxIter=" + INVERSE_MAX_ITERATIONS
				+ "|tol=" + INVERSE_TOLERANCE
				+ "|damping=" + INVERSE_DAMPING;
		final RealTransform cached = elastixBsplineCache.get( cacheKey );
		if ( cached != null )
			return cached;

		final RealTransform transform = createElastixBsplineRealTransform( parametersUri, inverse );
		elastixBsplineCache.put( cacheKey, transform );
		return transform;
	}

	private RealTransform createElastixBsplineRealTransform( String parametersUri, boolean inverse ) throws Exception
	{
		final ElastixBSplineTransform elastixTransform = ( ElastixBSplineTransform ) ElastixTransform.load( parametersUri );
		final RealTransform forwardTransform = ElastixBSplineToBSplineRealTransform.convert( elastixTransform );

		if ( inverse )
			return createIterativeInverse( forwardTransform );
		else
			return forwardTransform;
	}

	private RealTransform createIterativeInverse( final RealTransform forwardTransform )
	{
		IJ.log( "Computing inverse transform (iterative)..." );
		final long startTime = System.currentTimeMillis();
		final RealTransform inverseTransform = new InverseElastixBSplineRealTransform(
				forwardTransform,
				INVERSE_MAX_ITERATIONS,
				INVERSE_TOLERANCE,
				INVERSE_DAMPING
		);
		IJ.log( "...done in " + ( System.currentTimeMillis() - startTime ) + " ms." );
		return inverseTransform;
	}

	private RealTransform createPrecomputedDisplacementFieldInverse(
			final RealTransform forwardTransform,
			final ElastixBSplineTransform elastixTransform )
	{
		IJ.log( "Computing inverse transform (precomputed displacement field)..." );
		final long startTime = System.currentTimeMillis();

		final double[] min = ArrayUtils.toPrimitive( elastixTransform.GridOrigin );
		final double[] gridSpacing = ArrayUtils.toPrimitive( elastixTransform.GridSpacing );
		final int[] gridSize = ArrayUtils.toPrimitive( elastixTransform.GridSize );
		final double[] max = new double[ min.length ];
		for ( int d = 0; d < max.length; d++ )
			max[ d ] = min[ d ] + gridSpacing[ d ] * gridSize[ d ];

		final double[] spacing = Arrays.stream( gridSpacing )
				.map( x -> x / INVERSE_SAMPLING_FACTOR )
				.toArray();

		final RealTransform inverseTransform = new InverseDisplacementFieldTransformCreator(
				forwardTransform,
				min,
				max,
				spacing
		).get();

		IJ.log( "...done in " + ( System.currentTimeMillis() - startTime ) + " ms." );
		return inverseTransform;
	}

}
