package org.embl.mobie.command.create;

import ij.IJ;
import net.imglib2.RandomAccess;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.DisplacementFieldStorageMetadata;
import org.embl.mobie.lib.transform.DisplacementFieldTransformIO;
import org.embl.mobie.lib.transform.ElastixBSplineToBSplineRealTransform;
import org.embl.mobie.lib.transform.InverseDisplacementFieldTransformCreator;
import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;
import org.embl.mobie.lib.transform.elastix.ElastixTransform;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Create>Create Inverse Displacement Field From Elastix BSpline..." )
public class CreateInverseDisplacementFieldFromElastixBSplineCommand implements Command
{
	private static final int QUALITY_SAMPLES = 5000;
	private static final long QUALITY_RANDOM_SEED = 17L;

	@Parameter(label = "Elastix TransformParameters file")
	public File elastixTransformParametersFile;

	@Parameter(label = "Output displacement metadata JSON", style = "save")
	public File outputDisplacementFieldJson;

	@Parameter(label = "Sampling factor", min = "1")
	public int samplingFactor = 3;

	@Parameter(label = "Optimizer max step")
	public double optimizerMaxStep = 500.0;

	@Parameter(label = "Optimizer tolerance")
	public double optimizerTolerance = 0.5;

	@Parameter(label = "Optimizer max iterations")
	public int optimizerMaxIterations = 200;

	@Parameter(label = "Overwrite existing output")
	public boolean overwrite = false;

	@Parameter(label = "Comments", required = false)
	public String comments = "";

	@Override
	public void run()
	{
		try
		{
			if ( samplingFactor <= 0 )
				throw new IllegalArgumentException( "samplingFactor must be > 0" );
			if ( elastixTransformParametersFile == null || !elastixTransformParametersFile.exists() )
				throw new IllegalArgumentException( "Elastix file does not exist: " + elastixTransformParametersFile );
			if ( outputDisplacementFieldJson == null )
				throw new IllegalArgumentException( "Output JSON must be provided." );

			final File rawFile = deriveRawFile( outputDisplacementFieldJson );
			if ( !overwrite && ( outputDisplacementFieldJson.exists() || rawFile.exists() ) )
				throw new IllegalArgumentException( "Output already exists. Enable overwrite or choose another output path." );

			if ( outputDisplacementFieldJson.getParentFile() != null )
				outputDisplacementFieldJson.getParentFile().mkdirs();

			final ElastixBSplineTransform elastix = ( ElastixBSplineTransform ) ElastixTransform.load( elastixTransformParametersFile );
			if ( elastix.FixedImageDimension == null || elastix.FixedImageDimension != 3 )
				throw new IllegalArgumentException( "Only 3D Elastix BSpline transforms are supported." );

			final RealTransform forward = ElastixBSplineToBSplineRealTransform.convert( elastix );

			final double[] min = ArrayUtils.toPrimitive( elastix.GridOrigin );
			final double[] gridSpacing = ArrayUtils.toPrimitive( elastix.GridSpacing );
			final int[] gridSize = ArrayUtils.toPrimitive( elastix.GridSize );
			final double[] max = new double[ min.length ];
			for ( int d = 0; d < max.length; d++ )
				max[ d ] = min[ d ] + gridSpacing[ d ] * gridSize[ d ];

			final double[] samplingSpacing = Arrays.stream( gridSpacing )
					.map( x -> x / samplingFactor )
					.toArray();

			final long start = System.currentTimeMillis();
			IJ.log( "Sampling inverse displacement field..." );
			final int[] milestones = new int[] { 0, 20, 40, 60, 80, 100 };
			final int[] nextMilestoneIndex = new int[] { 0 };
			IJ.log( "Sampling progress: 0%" );
			final InverseDisplacementFieldTransformCreator.SampledInverseDisplacement sampled =
					new InverseDisplacementFieldTransformCreator(
							forward,
							min,
							max,
							samplingSpacing,
							optimizerMaxStep,
							optimizerTolerance,
							optimizerMaxIterations
					).sampleInverseDisplacement( percent -> {
						while ( nextMilestoneIndex[ 0 ] < milestones.length && percent >= milestones[ nextMilestoneIndex[ 0 ] ] )
						{
							final int milestone = milestones[ nextMilestoneIndex[ 0 ]++ ];
							if ( milestone != 0 )
								IJ.log( "Sampling progress: " + milestone + "%" );
						}
					} );

			final DisplacementFieldTransform inverse = new DisplacementFieldTransform(
					sampled.interleavedField,
					sampled.spacing,
					sampled.min );

			final Quality quality = computeQualityStats( forward, inverse, min, max, QUALITY_SAMPLES, QUALITY_RANDOM_SEED );
			final DisplacementStats displacement = computeDisplacementStats( sampled.interleavedField );

			final DisplacementFieldStorageMetadata metadata = new DisplacementFieldStorageMetadata();
			metadata.sourceElastixTransformParametersFile = elastixTransformParametersFile.getAbsolutePath();
			metadata.samplingFactor = samplingFactor;
			metadata.optimizerMaxStep = optimizerMaxStep;
			metadata.optimizerTolerance = optimizerTolerance;
			metadata.optimizerMaxIterations = optimizerMaxIterations;
			metadata.computeTimestamp = Instant.now().toString();
			metadata.comments = comments == null || comments.trim().isEmpty() ? null : comments.trim();

			metadata.quality = new DisplacementFieldStorageMetadata.QualityMetrics();
			metadata.quality.numSamples = quality.numSamples;
			metadata.quality.meanRoundTripError = quality.meanRoundTripError;
			metadata.quality.maxRoundTripError = quality.maxRoundTripError;

			metadata.displacement = new DisplacementFieldStorageMetadata.DisplacementStatistics();
			metadata.displacement.numSamples = displacement.numSamples;
			metadata.displacement.medianMagnitude = displacement.medianMagnitude;
			metadata.displacement.maxMagnitude = displacement.maxMagnitude;

			DisplacementFieldTransformIO.save(
					sampled.interleavedField,
					sampled.spacing,
					sampled.min,
					outputDisplacementFieldJson,
					metadata );

			IJ.log( "Saved inverse displacement field metadata: " + outputDisplacementFieldJson.getAbsolutePath() );
			IJ.log( "Saved inverse displacement field payload:  " + rawFile.getAbsolutePath() );
			IJ.log( "Inverse quality: samples=" + quality.numSamples
					+ ", meanRoundTripError=" + quality.meanRoundTripError
					+ ", maxRoundTripError=" + quality.maxRoundTripError );
			IJ.log( "Displacement stats: samples=" + displacement.numSamples
					+ ", medianMagnitude=" + displacement.medianMagnitude
					+ ", maxMagnitude=" + displacement.maxMagnitude );
			IJ.log( "Computed in " + ( System.currentTimeMillis() - start ) + " ms." );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( "Failed to create inverse displacement field from Elastix BSpline.", e );
		}
	}

	private static File deriveRawFile( final File jsonFile )
	{
		final String jsonName = jsonFile.getName();
		final int dot = jsonName.lastIndexOf( '.' );
		final String stem = dot > 0 ? jsonName.substring( 0, dot ) : jsonName;
		final File parent = jsonFile.getParentFile();
		if ( parent == null )
			return new File( stem + ".raw" );
		return new File( parent, stem + ".raw" );
	}

	private static Quality computeQualityStats(
			final RealTransform forward,
			final RealTransform inverse,
			final double[] sourceMin,
			final double[] sourceMax,
			final int samples,
			final long randomSeed )
	{
		final int n = sourceMin.length;
		final Random random = new Random( randomSeed );
		final double[] x = new double[ n ];
		final double[] y = new double[ n ];
		final double[] xRecovered = new double[ n ];

		double sumMaxAbsError = 0.0;
		double maxError = 0.0;

		for ( int i = 0; i < samples; i++ )
		{
			for ( int d = 0; d < n; d++ )
				x[ d ] = sourceMin[ d ] + random.nextDouble() * ( sourceMax[ d ] - sourceMin[ d ] );

			forward.apply( x, y );
			inverse.apply( y, xRecovered );

			double maxAbsError = 0.0;
			for ( int d = 0; d < n; d++ )
				maxAbsError = Math.max( maxAbsError, Math.abs( xRecovered[ d ] - x[ d ] ) );

			sumMaxAbsError += maxAbsError;
			maxError = Math.max( maxError, maxAbsError );
		}

		return new Quality( samples, samples <= 0 ? 0.0 : sumMaxAbsError / samples, maxError );
	}

	private static DisplacementStats computeDisplacementStats( final net.imglib2.RandomAccessibleInterval< ? extends RealType< ? > > interleaved )
	{
		final int n = ( int ) interleaved.dimension( 0 );
		if ( n != 3 )
			throw new IllegalArgumentException( "Only 3D displacement fields are supported." );

		final int[] size = new int[ n ];
		for ( int d = 0; d < n; d++ )
			size[ d ] = Math.toIntExact( interleaved.dimension( d + 1 ) );

		final RandomAccess< ? extends RealType< ? > > access = interleaved.randomAccess();
		final LocalizingIntervalIterator iterator = new LocalizingIntervalIterator( size );

		long totalSamplesLong = 1L;
		for ( int d = 0; d < n; d++ )
			totalSamplesLong *= size[ d ];
		if ( totalSamplesLong > Integer.MAX_VALUE )
			throw new IllegalArgumentException( "Displacement field too large for median computation: " + totalSamplesLong );
		final int totalSamples = ( int ) totalSamplesLong;
		final double[] magnitudes = new double[ totalSamples ];
		double maxMagnitude = 0.0;
		int sampleIndex = 0;

		while ( iterator.hasNext() )
		{
			iterator.fwd();
			double sq = 0.0;
			for ( int c = 0; c < n; c++ )
			{
				access.setPosition( c, 0 );
				for ( int d = 0; d < n; d++ )
					access.setPosition( iterator.getLongPosition( d ), d + 1 );
				final double value = access.get().getRealDouble();
				sq += value * value;
			}
			final double magnitude = Math.sqrt( sq );
			magnitudes[ sampleIndex++ ] = magnitude;
			maxMagnitude = Math.max( maxMagnitude, magnitude );
		}

		Arrays.sort( magnitudes );
		final double median = median( magnitudes, sampleIndex );

		return new DisplacementStats( sampleIndex, median, maxMagnitude );
	}

	private static double median( final double[] sortedValues, final int length )
	{
		if ( length <= 0 )
			return 0.0;
		if ( ( length & 1 ) == 1 )
			return sortedValues[ length / 2 ];
		final int hi = length / 2;
		final int lo = hi - 1;
		return 0.5 * ( sortedValues[ lo ] + sortedValues[ hi ] );
	}

	private static final class Quality
	{
		private final int numSamples;
		private final double meanRoundTripError;
		private final double maxRoundTripError;

		private Quality( final int numSamples, final double meanRoundTripError, final double maxRoundTripError )
		{
			this.numSamples = numSamples;
			this.meanRoundTripError = meanRoundTripError;
			this.maxRoundTripError = maxRoundTripError;
		}
	}

	private static final class DisplacementStats
	{
		private final int numSamples;
		private final double medianMagnitude;
		private final double maxMagnitude;

		private DisplacementStats( final int numSamples, final double medianMagnitude, final double maxMagnitude )
		{
			this.numSamples = numSamples;
			this.medianMagnitude = medianMagnitude;
			this.maxMagnitude = maxMagnitude;
		}
	}
}


