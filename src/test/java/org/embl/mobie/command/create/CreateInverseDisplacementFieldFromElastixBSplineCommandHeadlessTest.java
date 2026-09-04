package org.embl.mobie.command.create;

import bdv.cache.SharedQueue;
import bdv.viewer.Source;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateInverseDisplacementFieldFromElastixBSplineCommandHeadlessTest
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Test
	public void createsJsonRawAndOmeZarrAndCanOpenIt( @TempDir final Path tempDir ) throws Exception
	{
		final Path outputDir = resolveOutputDir( tempDir );
		Files.createDirectories( outputDir );

		final double[] originXYZ = new double[] { 5.0, 6.0, 7.0 };

		final File elastixWithUnit = createElastixTransformParameters(
				outputDir,
				20,
				20,
				20,
				new double[] { 2.0, 3.0, 4.0 },
				originXYZ,
				"micrometer" );

		final File outputJson = new File( outputDir.toFile(), "inverse-field.json" );
		final File outputRaw = new File( outputDir.toFile(), "inverse-field.raw" );
		final File outputOmeZarr = new File( outputDir.toFile(), "inverse-field.ome.zarr" );

		final CreateInverseDisplacementFieldFromElastixBSplineCommand cmd = new CreateInverseDisplacementFieldFromElastixBSplineCommand();
		cmd.elastixTransformParametersFile = elastixWithUnit;
		cmd.outputDisplacementFieldJson = outputJson;
		cmd.samplingFactor = 1;
		cmd.overwrite = true;
		cmd.comments = "test";
		cmd.run();

		assertTrue( outputJson.exists(), "JSON metadata should be created" );
		assertTrue( outputRaw.exists(), "RAW payload should be created" );
		assertTrue( outputOmeZarr.exists() && outputOmeZarr.isDirectory(), "OME-Zarr directory should be created" );
		assertTrue( new File( outputOmeZarr, ".zattrs" ).exists(), "OME-Zarr root .zattrs should exist" );

		final String s0Zattrs = new String(
				Files.readAllBytes( outputOmeZarr.toPath().resolve( "s0" ).resolve( ".zattrs" ) ),
				StandardCharsets.UTF_8 );

		final String uri = outputOmeZarr.getAbsolutePath();
		final ImageData< ? > imageData = ImageDataOpener.open(
				uri,
				ImageDataFormat.fromPath( uri ),
				new SharedQueue( 1 ) );
		assertNotNull( imageData );

		final Source< ? > source = imageData.getSourcePair( 0 ).getB();
		assertNotNull( source );

		assertEquals( "micrometer", source.getVoxelDimensions().unit() );
		assertArrayEquals( new double[] { 2.0, 3.0, 4.0 }, source.getVoxelDimensions().dimensionsAsDoubleArray(), 1e-6 );

		// TODO: think about this test.
		//  The issue is that this is now the origin in the sampled inverse space, which is actually different from the input origin because it contains the translation that happens during the B-Spline.
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );
		//assertEquals( originXYZ[ 0 ], sourceTransform.get( 0, 3 ), 1e-6 );
		//assertEquals( originXYZ[ 1 ], sourceTransform.get( 1, 3 ), 1e-6 );
		//assertEquals( originXYZ[ 2 ], sourceTransform.get( 2, 3 ), 1e-6 );

		System.out.println( "Wrote inverse-field test output to: " + outputOmeZarr.getAbsolutePath() );
	}

	private static Path resolveOutputDir( final Path defaultTempDir )
	{
		final String configuredOutputDir = "/Users/tischer/Downloads";
		if ( configuredOutputDir == null || configuredOutputDir.trim().isEmpty() )
			return defaultTempDir;

		return new File( configuredOutputDir ).toPath().toAbsolutePath();
	}

	private static File createElastixTransformParameters(
			final Path outputDir,
			final int gridX,
			final int gridY,
			final int gridZ,
			final double[] gridSpacing,
			final double[] gridOrigin,
			final String unit ) throws Exception
	{
		final long coefficientsPerDimension = ( long ) gridX * gridY * gridZ;
		final long numberOfParameters = coefficientsPerDimension * 3L;

		final StringBuilder params = new StringBuilder();
		for ( long i = 0; i < coefficientsPerDimension; i++ )
			params.append( "10.0 " ); // X
		for ( long i = 0; i < coefficientsPerDimension; i++ )
			params.append( "20.0 " ); // Y
		for ( long i = 0; i < coefficientsPerDimension; i++ )
			params.append( "30.0 " ); // Z

		final String content = "(Transform \"BSplineTransform\")\n"
				+ "(NumberOfParameters " + numberOfParameters + ")\n"
				+ "(TransformParameters " + params.toString().trim() + ")\n"
				+ "(InitialTransformParametersFileName \"NoInitialTransform\")\n"
				+ "(HowToCombineTransforms \"Compose\")\n\n"
				+ "// Image specific\n"
				+ "(FixedImageDimension 3)\n"
				+ "(MovingImageDimension 3)\n"
				+ "(FixedInternalImagePixelType \"float\")\n"
				+ "(MovingInternalImagePixelType \"float\")\n"
				+ "(Size 100 100 100)\n"
				+ "(Index 0 0 0)\n"
				+ "(Spacing " + gridSpacing[ 0 ] + " " + gridSpacing[ 1 ] + " " + gridSpacing[ 2 ] + ")\n"
				+ "(Origin " + gridOrigin[ 0 ] + " " + gridOrigin[ 1 ] + " " + gridOrigin[ 2 ] + ")\n"
				+ "(Direction 1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0)\n"
				+ "(UseDirectionCosines \"true\")\n\n"
				+ "// BSplineTransform specific\n"
				+ "(GridSize " + gridX + " " + gridY + " " + gridZ + ")\n"
				+ "(GridIndex 0 0 0)\n"
				+ "(GridSpacing " + gridSpacing[ 0 ] + " " + gridSpacing[ 1 ] + " " + gridSpacing[ 2 ] + ")\n"
				+ "(GridOrigin " + gridOrigin[ 0 ] + " " + gridOrigin[ 1 ] + " " + gridOrigin[ 2 ] + ")\n"
				+ "(GridDirection 1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0)\n"
				+ "(BSplineTransformSplineOrder 3)\n"
				+ "(UseCyclicTransform \"false\")\n"
				+ "(Unit \"" + unit + "\")\n\n"
				+ "// ResampleInterpolator specific\n"
				+ "(ResampleInterpolator \"FinalBSplineInterpolator\")\n"
				+ "(FinalBSplineInterpolationOrder 3)\n\n"
				+ "// Resampler specific\n"
				+ "(Resampler \"DefaultResampler\")\n"
				+ "(DefaultPixelValue 0.0)\n"
				+ "(ResultImageFormat \"mhd\")\n"
				+ "(ResultImagePixelType \"unsigned char\")\n"
				+ "(CompressResultImage \"false\")\n";

		final File elastixFile = new File( outputDir.toFile(), "TransformParameters.Grid20.WithUnit.txt" );
		Files.write( elastixFile.toPath(), content.getBytes( StandardCharsets.UTF_8 ) );
		return elastixFile;
	}
}




