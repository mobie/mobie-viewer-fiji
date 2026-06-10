package org.embl.mobie.lib.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.embl.mobie.io.util.IOHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * IO utilities for displacement fields persisted as JSON metadata and raw float payload.
 */
public class DisplacementFieldTransformIO
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void save(
			final RandomAccessibleInterval< ? extends RealType< ? > > interleavedField,
			final double[] spacing,
			final double[] origin,
			final File jsonFile ) throws IOException
	{
		save( interleavedField, spacing, origin, jsonFile, null );
	}

	public static void save(
			final RandomAccessibleInterval< ? extends RealType< ? > > interleavedField,
			final double[] spacing,
			final double[] origin,
			final File jsonFile,
			final DisplacementFieldStorageMetadata metadataTemplate ) throws IOException
	{
		if ( interleavedField.numDimensions() < 2 )
			throw new IllegalArgumentException( "Interleaved displacement field must have at least 2 dimensions." );

		final int numDimensions = ( int ) interleavedField.dimension( 0 );
		if ( numDimensions != 3 )
			throw new IllegalArgumentException( "Only 3D displacement fields are supported, got component count=" + numDimensions );

		if ( spacing.length != numDimensions || origin.length != numDimensions )
			throw new IllegalArgumentException( "spacing and origin dimensionality must match field dimensionality." );

		final int[] size = new int[ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
			size[ d ] = Math.toIntExact( interleavedField.dimension( d + 1 ) );

		final String jsonName = jsonFile.getName();
		final int dot = jsonName.lastIndexOf( '.' );
		final String stem = dot > 0 ? jsonName.substring( 0, dot ) : jsonName;
		final String rawName = stem + ".raw";

		final DisplacementFieldStorageMetadata metadata =
				metadataTemplate == null ? new DisplacementFieldStorageMetadata() : metadataTemplate;
		metadata.numDimensions = numDimensions;
		metadata.size = Arrays.copyOf( size, size.length );
		metadata.spacing = Arrays.copyOf( spacing, spacing.length );
		metadata.origin = Arrays.copyOf( origin, origin.length );
		metadata.rawPath = rawName;

		if ( jsonFile.getParentFile() != null )
			jsonFile.getParentFile().mkdirs();

		try ( OutputStreamWriter writer = new OutputStreamWriter( new FileOutputStream( jsonFile ), StandardCharsets.UTF_8 ) )
		{
			GSON.toJson( metadata, writer );
		}

		final Path rawPath = resolveRawPath( jsonFile.toPath(), metadata.rawPath );
		try ( DataOutputStream output = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( rawPath.toFile() ) ) ) )
		{
			for ( final RealType< ? > value : Views.flatIterable( interleavedField ) )
				output.writeFloat( value.getRealFloat() );
		}
	}

	public static DisplacementFieldTransform load( final String jsonUri ) throws IOException
	{
		final DisplacementFieldStorageMetadata metadata;
		try ( InputStreamReader reader = new InputStreamReader( IOHelper.getInputStream( jsonUri ), StandardCharsets.UTF_8 ) )
		{
			metadata = GSON.fromJson( reader, DisplacementFieldStorageMetadata.class );
		}

		validateMetadata( metadata, jsonUri );

		String rawUri = IOHelper.combinePath( IOHelper.getParentLocation( jsonUri ), metadata.rawPath );

		long valuesCount = metadata.numDimensions;
		for ( int d = 0; d < metadata.numDimensions; d++ )
			valuesCount *= metadata.size[ d ];

		if ( valuesCount > Integer.MAX_VALUE )
			throw new IOException( "Displacement field too large for in-memory float array: " + valuesCount );

		final float[] values = new float[ ( int ) valuesCount ];
		try ( DataInputStream input = new DataInputStream( new BufferedInputStream( IOHelper.getInputStream( rawUri ) ) ) )
		{
			for ( int i = 0; i < values.length; i++ )
				values[ i ] = input.readFloat();
		}

		final long[] dims = new long[ metadata.numDimensions + 1 ];
		dims[ 0 ] = metadata.numDimensions;
		for ( int d = 0; d < metadata.numDimensions; d++ )
			dims[ d + 1 ] = metadata.size[ d ];

		final RandomAccessibleInterval< FloatType > interleaved = net.imglib2.img.array.ArrayImgs.floats( values, dims );
		return new DisplacementFieldTransform( interleaved, metadata.spacing, metadata.origin );
	}

	private static void validateMetadata( final DisplacementFieldStorageMetadata metadata, final String uri ) throws IOException
	{
		if ( metadata == null )
			throw new IOException( "Could not parse displacement metadata JSON: " + uri );
		if ( metadata.numDimensions != 3 )
			throw new IOException( "Only 3D displacement fields are supported, got numDimensions=" + metadata.numDimensions );
		if ( metadata.componentAxis != 0 )
			throw new IOException( "Unsupported componentAxis=" + metadata.componentAxis + ", expected 0" );
		if ( metadata.size == null || metadata.spacing == null || metadata.origin == null || metadata.rawPath == null )
			throw new IOException( "Incomplete displacement metadata in: " + uri );
		if ( metadata.size.length != metadata.numDimensions || metadata.spacing.length != metadata.numDimensions || metadata.origin.length != metadata.numDimensions )
			throw new IOException( "Metadata dimensionality mismatch in: " + uri );
	}

	private static Path resolveRawPath( final Path jsonPath, final String rawPath )
	{
		final Path raw = new File( rawPath ).toPath();
		if ( raw.isAbsolute() )
			return raw;
		final Path parent = jsonPath.getParent();
		return parent == null ? raw : parent.resolve( rawPath );
	}
}

