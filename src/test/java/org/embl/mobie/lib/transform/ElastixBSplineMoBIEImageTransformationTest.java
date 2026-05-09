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
	void samplesTransformedMoBIEImageWithoutUI() throws Exception
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
		final RealRandomAccess< ? > access = transformedImage.getSourcePair()
				.getSource()
				.getInterpolatedSource( 0, 0, Interpolation.NEARESTNEIGHBOR )
				.realRandomAccess();

		access.setPosition( new double[] { 10, 20, 30 } );
		final int valueAt102030 = ( ( UnsignedByteType ) access.get() ).get();

		access.setPosition( new double[] { 30, 5, 10 } );
		final int valueAt30510 = ( ( UnsignedByteType ) access.get() ).get();

		Assertions.assertEquals( 20, valueAt102030 );
		Assertions.assertEquals( 40, valueAt30510 );
	}
}
