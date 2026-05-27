package org.embl.mobie.lib.transform;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import org.embl.mobie.lib.image.DefaultImage;
import org.embl.mobie.lib.image.DefaultSourcePair;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.transformation.ElastixBSplineTransformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Collections;

class ElastixBSplineMoBIEImageTransformationTest
{
	@Test
	void sampleBSplineTransformedImage() throws Exception
	{
		final long[] dims = new long[] { 64, 64, 64 };
		final RandomAccessibleInterval< UnsignedByteType > imageData = ArrayImgs.unsignedBytes( dims );
		final net.imglib2.RandomAccess< UnsignedByteType > imageAccess = imageData.randomAccess();
		for ( int z = 0; z < dims[ 2 ]; z++ )
			for ( int y = 0; y < dims[ 1 ]; y++ )
				for ( int x = 0; x < dims[ 0 ]; x++ )
				{
					imageAccess.setPosition( new long[] { x, y, z } );
					imageAccess.get().set( x );
				}

		final Source< UnsignedByteType > source = new RandomAccessibleIntervalSource<>(
				imageData,
				new UnsignedByteType(),
				new AffineTransform3D(),
				"source" );

		final RandomAccessibleInterval< VolatileUnsignedByteType > volatileData = Converters.convert(
				imageData,
				( input, output ) ->
				{
					output.set( input.get() );
					output.setValid( true );
				},
				new VolatileUnsignedByteType() );
		final Source< VolatileUnsignedByteType > volatileSource = new RandomAccessibleIntervalSource<>(
				volatileData,
				new VolatileUnsignedByteType(),
				new AffineTransform3D(),
				"source-volatile" );

		final Image< UnsignedByteType > image = new DefaultImage<>(
				"source",
				new DefaultSourcePair<>( source, volatileSource ),
				null );

		final URL transformResource = ElastixBSplineMoBIEImageTransformationTest.class
				.getResource( "/elastix/TransformParameters.BSpline3D.TranslationX.txt" );
		Assertions.assertNotNull( transformResource, "Missing 3D Elastix transform test resource" );

		final ElastixBSplineTransformation transformation = new ElastixBSplineTransformation(
				"bspline-3d",
				new File( transformResource.toURI() ).getAbsolutePath(),
				Collections.singletonList( "source" ),
				Collections.singletonList( "source-bspline" ) );

		final Image< ? > transformedImage = ImageTransformer.elastixBSplineTransform( image, transformation );
		final Source< ? > transformedSource = transformedImage.getSourcePair().getSource();

		final int valueAt102030 = sampleNearestAtGlobalPosition( transformedSource, new double[] { 10, 20, 30 } );
		final int valueAt30510 = sampleNearestAtGlobalPosition( transformedSource, new double[] { 30, 5, 10 } );

		Assertions.assertEquals( 20, valueAt102030 );
		Assertions.assertEquals( 40, valueAt30510 );
	}

	@Test
	void samplesTransformedMoBIEImageWithNonIdentitySourceTransform() throws Exception
	{
		final long[] dims = new long[] { 256, 64, 64 };
		final RandomAccessibleInterval< UnsignedByteType > imageData = ArrayImgs.unsignedBytes( dims );
		final net.imglib2.RandomAccess< UnsignedByteType > imageAccess = imageData.randomAccess();
		for ( int z = 0; z < dims[ 2 ]; z++ )
			for ( int y = 0; y < dims[ 1 ]; y++ )
				for ( int x = 0; x < dims[ 0 ]; x++ )
				{
					imageAccess.setPosition( new long[] { x, y, z } );
					imageAccess.get().set( x );
				}

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.scale( 2.0, 1.0, 1.0 );
		sourceTransform.translate( 100.0, 0.0, 0.0 );

		final Source< UnsignedByteType > source = new RandomAccessibleIntervalSource<>(
				imageData,
				new UnsignedByteType(),
				sourceTransform,
				"source" );

		final RandomAccessibleInterval< VolatileUnsignedByteType > volatileData = Converters.convert(
				imageData,
				( input, output ) ->
				{
					output.set( input.get() );
					output.setValid( true );
				},
				new VolatileUnsignedByteType() );
		final Source< VolatileUnsignedByteType > volatileSource = new RandomAccessibleIntervalSource<>(
				volatileData,
				new VolatileUnsignedByteType(),
				sourceTransform,
				"source-volatile" );

		final Image< UnsignedByteType > image = new DefaultImage<>(
				"source",
				new DefaultSourcePair<>( source, volatileSource ),
				null );

		final URL transformResource = ElastixBSplineMoBIEImageTransformationTest.class
				.getResource( "/elastix/TransformParameters.BSpline3D.TranslationX.txt" );
		Assertions.assertNotNull( transformResource, "Missing 3D Elastix transform test resource" );

		final ElastixBSplineTransformation transformation = new ElastixBSplineTransformation(
				"bspline-3d",
				new File( transformResource.toURI() ).getAbsolutePath(),
				Collections.singletonList( "source" ),
				Collections.singletonList( "source-bspline" ) );

		final Image< ? > transformedImage = ImageTransformer.elastixBSplineTransform( image, transformation );
		final Source< ? > transformedSource = transformedImage.getSourcePair().getSource();

		// +10 mm in world x should increase sampled local voxel x by +10 / scaleX = +5.
		// Here: x_global = 2 * x_local + 100, so x_local = (x_global - 100) / 2.
		// Therefore:
		//   120 -> warped world 130 -> local (130 - 100) / 2 = 15
		//   140 -> warped world 150 -> local (150 - 100) / 2 = 25
		final int valueAt1202030 = sampleNearestAtGlobalPosition( transformedSource, new double[] { 120, 20, 30 } );
		final int valueAt140510 = sampleNearestAtGlobalPosition( transformedSource, new double[] { 140, 5, 10 } );

		Assertions.assertEquals( 15, valueAt1202030 );
		Assertions.assertEquals( 25, valueAt140510 );
	}

	private static int sampleNearestAtGlobalPosition( final Source< ? > source, final double[] globalPosition )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );

		final double[] sourceLocalPosition = new double[ globalPosition.length ];
		sourceTransform.inverse().apply( globalPosition, sourceLocalPosition );

		final RealRandomAccess< ? > access = source
				.getInterpolatedSource( 0, 0, Interpolation.NEARESTNEIGHBOR )
				.realRandomAccess();
		access.setPosition( sourceLocalPosition );
		return ( ( UnsignedByteType ) access.get() ).get();
	}
}
