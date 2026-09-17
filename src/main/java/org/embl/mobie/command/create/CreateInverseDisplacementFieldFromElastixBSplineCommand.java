package org.embl.mobie.command.create;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.lib.transform.DisplacementFieldStorageMetadata;
import org.embl.mobie.lib.transform.DisplacementFieldTransformIO;
import org.embl.mobie.lib.transform.ElastixBSplineToBSplineRealTransform;
import org.embl.mobie.lib.transform.InverseDisplacementFieldTransformCreator;
import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;
import org.embl.mobie.lib.transform.elastix.ElastixTransform;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Create>Create Inverse Displacement Field From Elastix BSpline..." )
public class CreateInverseDisplacementFieldFromElastixBSplineCommand implements Command
{
	private static final int QUALITY_SAMPLES = 5000;
	private static final long QUALITY_RANDOM_SEED = 17L;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DEFAULT_PHYSICAL_UNIT = "pixel";
	private static final Set< String > ELASTIX_UNIT_KEYS = new HashSet<>( Arrays.asList(
			"Unit",
			"Units",
			"SpacingUnit",
			"SpacingUnits",
			"LengthUnit",
			"LengthUnits",
			"GridSpacingUnit",
			"GridSpacingUnits" ) );

	@Parameter(label = "Elastix TransformParameters file")
	public File elastixTransformParametersFile;

	@Parameter(label = "Output displacement metadata JSON", style = "save")
	public File outputDisplacementFieldJson;

	@Parameter(label = "Sampling factor", min = "1", description = "Higher values will yield a more accurate transformation.\n" +
			"To check the accuracy run this command and see ImageJ Log Window: meanRoundTripError (smaller is better). ")
	public int samplingFactor = 3;

	//@Parameter(label = "Optimizer max step")
	public double optimizerMaxStep = 500.0;

	//@Parameter(label = "Optimizer tolerance")
	public double optimizerTolerance = 0.5;

	//@Parameter(label = "Optimizer max iterations")
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
			final File omeZarrDirectory = deriveOmeZarrDirectory( outputDisplacementFieldJson );
			if ( !overwrite && ( outputDisplacementFieldJson.exists() || rawFile.exists() || omeZarrDirectory.exists() ) )
				throw new IllegalArgumentException( "Output already exists. Enable overwrite or choose another output path." );

			if ( outputDisplacementFieldJson.getParentFile() != null )
				outputDisplacementFieldJson.getParentFile().mkdirs();

			final ElastixBSplineTransform elastix = ( ElastixBSplineTransform ) ElastixTransform.load( elastixTransformParametersFile.getAbsolutePath() );
			if ( elastix.FixedImageDimension == null || elastix.FixedImageDimension != 3 )
				throw new IllegalArgumentException( "Only 3D Elastix BSpline transforms are supported." );
			final String physicalUnit = inferPhysicalUnitFromElastixFile( elastixTransformParametersFile );

			final RealTransform forward = ElastixBSplineToBSplineRealTransform.convert( elastix );

			final double[] gridOrigin = ArrayUtils.toPrimitive( elastix.GridOrigin );
			final double[] gridSpacing = ArrayUtils.toPrimitive( elastix.GridSpacing );
			final int[] gridSize = ArrayUtils.toPrimitive( elastix.GridSize );
			final double[] max = new double[ gridOrigin.length ];
			for ( int d = 0; d < max.length; d++ )
				max[ d ] = gridOrigin[ d ] + gridSpacing[ d ] * gridSize[ d ];

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
							gridOrigin,
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

			final Quality quality = computeQualityStats( forward, inverse, gridOrigin, max, QUALITY_SAMPLES, QUALITY_RANDOM_SEED );
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

			writeOmeZarrDisplacementField(
					sampled.interleavedField,
					sampled.spacing,
					sampled.min,
					physicalUnit,
					omeZarrDirectory,
					overwrite );

			IJ.log( "Saved inverse displacement field metadata: " + outputDisplacementFieldJson.getAbsolutePath() );
			IJ.log( "Saved inverse displacement field payload:  " + rawFile.getAbsolutePath() );
			IJ.log( "Saved inverse displacement field OME-Zarr: " + omeZarrDirectory.getAbsolutePath() );
			IJ.log( "OME-Zarr physical unit: " + physicalUnit );
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

	private static File deriveOmeZarrDirectory( final File jsonFile )
	{
		final String jsonName = jsonFile.getName();
		final int dot = jsonName.lastIndexOf( '.' );
		final String stem = dot > 0 ? jsonName.substring( 0, dot ) : jsonName;
		final File parent = jsonFile.getParentFile();
		if ( parent == null )
			return new File( stem + ".ome.zarr" );
		return new File( parent, stem + ".ome.zarr" );
	}

	private static String inferPhysicalUnitFromElastixFile( final File elastixFile ) throws IOException
	{
		final Pattern pattern = Pattern.compile( "\\((\\S+)\\s+(.+?)\\)" );
		final List< String > lines = Files.readAllLines( elastixFile.toPath(), StandardCharsets.UTF_8 );
		for ( final String line : lines )
		{
			final Matcher matcher = pattern.matcher( line.trim() );
			if ( !matcher.matches() )
				continue;
			if ( !ELASTIX_UNIT_KEYS.contains( matcher.group( 1 ) ) )
				continue;

			String unit = matcher.group( 2 ).trim();
			if ( unit.startsWith( "\"" ) && unit.endsWith( "\"" ) && unit.length() >= 2 )
				unit = unit.substring( 1, unit.length() - 1 ).trim();
			if ( !unit.isEmpty() )
				return unit;
		}

		IJ.log( "No unit key found in elastix TransformParameters file; using fallback unit: " + DEFAULT_PHYSICAL_UNIT );
		return DEFAULT_PHYSICAL_UNIT;
	}

	private static void writeOmeZarrDisplacementField(
			final RandomAccessibleInterval< ? extends RealType< ? > > interleavedField,
			final double[] spacing,
			final double[] origin,
			final String unit,
			final File outputOmeZarr,
			final boolean overwrite )
	{
		final ImagePlus displacementImage = asDisplacementImagePlus( interleavedField, spacing, origin, unit );

		OMEZarrWriter.write(
				displacementImage,
				outputOmeZarr.getAbsolutePath(),
				OMEZarrWriter.ImageType.Intensities,
				overwrite,
				N5ScalePyramidExporter.ZSTD_COMPRESSION );

		try
		{
			patchOmeZarrTranslationsWithOrigin( outputOmeZarr.toPath(), origin );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to patch OME-Zarr translation metadata.", e );
		}
	}

	private static void patchOmeZarrTranslationsWithOrigin( final Path omeZarrDirectory, final double[] origin ) throws IOException
	{
		if ( origin == null || origin.length < 3 )
			return;

		try ( Stream< Path > paths = Files.walk( omeZarrDirectory ) )
		{
			paths
					.filter( p -> p.getFileName().toString().equals( ".zattrs" ) )
					.forEach( p -> {
						try
						{
							patchSingleZattrsFile( p, origin );
						}
						catch ( IOException e )
						{
							throw new RuntimeException( e );
						}
					} );
		}
	}

	private static void patchSingleZattrsFile( final Path zattrsPath, final double[] origin ) throws IOException
	{
		final String content = new String( Files.readAllBytes( zattrsPath ), StandardCharsets.UTF_8 );
		final JsonObject root = GSON.fromJson( content, JsonObject.class );
		if ( root == null )
			return;

		boolean changed = false;

		if ( root.has( "multiscales" ) && root.get( "multiscales" ).isJsonArray() )
		{
			final JsonArray multiscales = root.getAsJsonArray( "multiscales" );
			for ( final JsonElement msElement : multiscales )
			{
				if ( !msElement.isJsonObject() )
					continue;
				final JsonObject multiscale = msElement.getAsJsonObject();
				final JsonArray axes = multiscale.has( "axes" ) && multiscale.get( "axes" ).isJsonArray()
						? multiscale.getAsJsonArray( "axes" )
						: null;

				if ( axes == null )
					continue;

				if ( multiscale.has( "datasets" ) && multiscale.get( "datasets" ).isJsonArray() )
				{
					final JsonArray datasets = multiscale.getAsJsonArray( "datasets" );
					for ( final JsonElement dsElement : datasets )
					{
						if ( !dsElement.isJsonObject() )
							continue;
						final JsonObject dataset = dsElement.getAsJsonObject();
						final String levelLabel = dataset.has( "path" ) ? dataset.get( "path" ).getAsString() : "unknown";
						changed |= patchCoordinateTransformations( dataset, axes, origin, levelLabel );
					}
				}
			}
		}

		if ( root.has( "axes" ) && root.get( "axes" ).isJsonArray() )
		{
			final Path parent = zattrsPath.getParent();
			final String levelLabel = parent == null ? "root" : parent.getFileName().toString();
			changed |= patchCoordinateTransformations( root, root.getAsJsonArray( "axes" ), origin, levelLabel );
		}

		if ( changed )
			Files.write( zattrsPath, GSON.toJson( root ).getBytes( StandardCharsets.UTF_8 ) );
	}

	private static boolean patchCoordinateTransformations(
			final JsonObject object,
			final JsonArray axes,
			final double[] origin,
			final String levelLabel )
	{
		if ( !object.has( "coordinateTransformations" ) || !object.get( "coordinateTransformations" ).isJsonArray() )
			return false;

		final JsonArray transforms = object.getAsJsonArray( "coordinateTransformations" );
		final double[] axisOffset = axisOffsets( axes, origin );
		if ( axisOffset == null )
			return false;

		for ( int i = 0; i < transforms.size(); i++ )
		{
			final JsonElement tElement = transforms.get( i );
			if ( !tElement.isJsonObject() )
				continue;
			final JsonObject transform = tElement.getAsJsonObject();
			if ( !transform.has( "type" ) || !"translation".equals( transform.get( "type" ).getAsString() ) )
				continue;
			if ( !transform.has( "translation" ) || !transform.get( "translation" ).isJsonArray() )
				continue;

			final JsonArray translation = transform.getAsJsonArray( "translation" );
			if ( translation.size() != axisOffset.length )
				continue;

			IJ.log( "OME-Zarr patch level=" + levelLabel
					+ ", axes=" + axesToString( axes )
					+ ", translation before=" + jsonArrayToString( translation ) );

			for ( int a = 0; a < translation.size(); a++ )
			{
				final double updated = translation.get( a ).getAsDouble() + axisOffset[ a ];
				translation.set( a, GSON.toJsonTree( updated ) );
			}

			IJ.log( "OME-Zarr patch level=" + levelLabel
					+ ", translation after=" + jsonArrayToString( translation ) );
			return true;
		}

		final JsonObject newTranslation = new JsonObject();
		newTranslation.addProperty( "type", "translation" );
		final JsonArray values = new JsonArray();
		for ( final double offset : axisOffset )
			values.add( offset );
		newTranslation.add( "translation", values );
		transforms.add( newTranslation );
		IJ.log( "OME-Zarr patch level=" + levelLabel
				+ ", axes=" + axesToString( axes )
				+ ", translation before=<missing>, after=" + jsonArrayToString( values ) );
		return true;
	}

	private static String jsonArrayToString( final JsonArray values )
	{
		final StringBuilder builder = new StringBuilder( "[" );
		for ( int i = 0; i < values.size(); i++ )
		{
			if ( i > 0 )
				builder.append( ", " );
			builder.append( values.get( i ) );
		}
		builder.append( "]" );
		return builder.toString();
	}

	private static String axesToString( final JsonArray axes )
	{
		final StringBuilder builder = new StringBuilder( "[" );
		for ( int i = 0; i < axes.size(); i++ )
		{
			if ( i > 0 )
				builder.append( ", " );
			final JsonElement axisElement = axes.get( i );
			if ( axisElement.isJsonObject() && axisElement.getAsJsonObject().has( "name" ) )
				builder.append( axisElement.getAsJsonObject().get( "name" ).getAsString() );
			else
				builder.append( "?" );
		}
		builder.append( "]" );
		return builder.toString();
	}

	private static double[] axisOffsets( final JsonArray axes, final double[] origin )
	{
		final double[] offsets = new double[ axes.size() ];
		for ( int i = 0; i < axes.size(); i++ )
		{
			final JsonElement axisElement = axes.get( i );
			if ( !axisElement.isJsonObject() )
				return null;
			final JsonObject axisObject = axisElement.getAsJsonObject();
			if ( !axisObject.has( "name" ) )
				return null;

			final String name = axisObject.get( "name" ).getAsString();
			switch ( name )
			{
				case "x":
					offsets[ i ] = origin[ 0 ];
					break;
				case "y":
					offsets[ i ] = origin[ 1 ];
					break;
				case "z":
					offsets[ i ] = origin[ 2 ];
					break;
				default:
					offsets[ i ] = 0.0;
			}
		}
		return offsets;
	}

	private static ImagePlus asDisplacementImagePlus(
			final RandomAccessibleInterval< ? extends RealType< ? > > interleavedField,
			final double[] spacing,
			final double[] origin,
			final String unit )
	{
		if ( interleavedField.numDimensions() != 4 )
			throw new IllegalArgumentException( "Expected interleaved displacement field dimensions [c,x,y,z]." );

		final int channels = Math.toIntExact( interleavedField.dimension( 0 ) );
		if ( channels != 3 )
			throw new IllegalArgumentException( "Expected 3 displacement channels (X,Y,Z), got: " + channels );

		final int sizeX = Math.toIntExact( interleavedField.dimension( 1 ) );
		final int sizeY = Math.toIntExact( interleavedField.dimension( 2 ) );
		final int sizeZ = Math.toIntExact( interleavedField.dimension( 3 ) );
		final ImageStack stack = new ImageStack( sizeX, sizeY );
		final RandomAccess< ? extends RealType< ? > > access = interleavedField.randomAccess();

		for ( int z = 0; z < sizeZ; z++ )
		{
			for ( int c = 0; c < channels; c++ )
			{
				final float[] pixels = new float[ sizeX * sizeY ];
				int i = 0;
				for ( int y = 0; y < sizeY; y++ )
				{
					for ( int x = 0; x < sizeX; x++ )
					{
						access.setPosition( c, 0 );
						access.setPosition( x, 1 );
						access.setPosition( y, 2 );
						access.setPosition( z, 3 );
						pixels[ i++ ] = access.get().getRealFloat();
					}
				}
				stack.addSlice( displacementChannelName( c ), pixels );
			}
		}

		final ImagePlus imagePlus = new ImagePlus( "inverse_displacement_field", stack );
		imagePlus.setDimensions( channels, sizeZ, 1 );
		imagePlus.setOpenAsHyperStack( true );

		final Calibration calibration = imagePlus.getCalibration();
		calibration.pixelWidth = spacing[ 0 ];
		calibration.pixelHeight = spacing[ 1 ];
		calibration.pixelDepth = spacing[ 2 ];
		// TODO: once https://github.com/saalfeldlab/n5-ij/issues/126 is fixed
		//  we can remove the comments and then also remove
		//  the patchOmeZarrTranslationsWithOrigin() function
//		calibration.xOrigin = origin[ 0 ];
//		calibration.yOrigin = origin[ 1 ];
//		calibration.zOrigin = origin[ 2 ];
		calibration.setUnit( unit );
		calibration.setXUnit( unit );
		calibration.setYUnit( unit );
		calibration.setZUnit( unit );

		return imagePlus;
	}

	private static String displacementChannelName( final int channel )
	{
		switch ( channel )
		{
			case 0:
				return "dx";
			case 1:
				return "dy";
			case 2:
				return "dz";
			default:
				return "d" + channel;
		}
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


