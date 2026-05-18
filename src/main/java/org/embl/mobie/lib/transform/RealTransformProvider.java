package org.embl.mobie.lib.transform;

import ij.IJ;
import net.imglib2.realtransform.RealTransform;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.lib.serialize.transformation.DisplacementFieldTransformation;
import org.embl.mobie.lib.serialize.transformation.ElastixBSplineTransformation;
import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;
import org.embl.mobie.lib.transform.elastix.ElastixTransform;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RealTransformProvider
{
	private enum InverseMode
	{
		ITERATIVE,
		PRECOMPUTED_DISPLACEMENT_FIELD
	}

	private static final int INVERSE_SAMPLING_FACTOR = 1;
	private static final int INVERSE_MAX_ITERATIONS = 80;
	private static final double INVERSE_TOLERANCE = 1e-6;
	private static final double INVERSE_DAMPING = 1.0;
	private static final InverseMode INVERSE_MODE = readInverseMode();

	private final Map< String, RealTransform > elastixBsplineCache = new ConcurrentHashMap<>();
	private final Map< String, RealTransform > displacementFieldCache = new ConcurrentHashMap<>();

	public RealTransform getDisplacementFieldRealTransform( final DisplacementFieldTransformation transformation ) throws Exception
	{
		final String cacheKey = transformation.getDisplacementFieldUri();
		final RealTransform cached = displacementFieldCache.get( cacheKey );
		if ( cached != null )
			return cached;

		final File jsonFile = new File( transformation.getDisplacementFieldUri() );
		if ( !jsonFile.exists() )
			throw new IllegalArgumentException( "displacement_field_uri does not exist: " + jsonFile.getAbsolutePath() );

		final RealTransform transform = DisplacementFieldTransformIO.load( jsonFile );
		displacementFieldCache.put( cacheKey, transform );
		return transform;
	}

	public RealTransform getElastixBSplineRealTransform( final ElastixBSplineTransformation transformation, final boolean invert ) throws Exception
	{
		final String cacheKey = transformation.getTransformParametersFile()
				+ "|invert=" + invert
				+ "|inverseType=" + INVERSE_MODE
				+ "|samplingFactor=" + INVERSE_SAMPLING_FACTOR
				+ "|maxIter=" + INVERSE_MAX_ITERATIONS
				+ "|tol=" + INVERSE_TOLERANCE
				+ "|damping=" + INVERSE_DAMPING;
		final RealTransform cached = elastixBsplineCache.get( cacheKey );
		if ( cached != null )
			return cached;

		final RealTransform transform = createElastixBsplineRealTransform( transformation, invert );
		elastixBsplineCache.put( cacheKey, transform );
		return transform;
	}

	private RealTransform createElastixBsplineRealTransform( final ElastixBSplineTransformation transformation, final boolean invert ) throws Exception
	{
		final File transformFile = new File( transformation.getTransformParametersFile() );
		final ElastixBSplineTransform elastixTransform = ( ElastixBSplineTransform ) ElastixTransform.load( transformFile );
		final RealTransform forwardTransform = ElastixBSplineToBSplineRealTransform.convert( elastixTransform );

		if ( !invert )
			return forwardTransform;

		if ( INVERSE_MODE == InverseMode.PRECOMPUTED_DISPLACEMENT_FIELD )
			return createPrecomputedDisplacementFieldInverse( forwardTransform, elastixTransform );
		return createIterativeInverse( forwardTransform );
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

	private static InverseMode readInverseMode()
	{
		final String value = System.getProperty( "mobie.elastix.inverse.mode", "PRECOMPUTED_DISPLACEMENT_FIELD" ).trim();
		try
		{
			return InverseMode.valueOf( value.toUpperCase() );
		}
		catch ( final IllegalArgumentException exception )
		{
			IJ.log( "Unknown mobie.elastix.inverse.mode='" + value + "', falling back to PRECOMPUTED_DISPLACEMENT_FIELD" );
			return InverseMode.PRECOMPUTED_DISPLACEMENT_FIELD;
		}
	}
}
