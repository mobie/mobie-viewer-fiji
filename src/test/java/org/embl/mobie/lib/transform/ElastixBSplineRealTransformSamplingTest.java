package org.embl.mobie.lib.transform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.embl.mobie.lib.transform.elastix.ElastixBSplineTransform;
import org.embl.mobie.lib.transform.elastix.ElastixTransform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

class ElastixBSplineRealTransformSamplingTest
{
	@Test
	void samplesExpectedPixelValuesAfterBSplineTransform() throws Exception
	{
		final long[] dims = new long[] { 64, 64 };
		final RandomAccessibleInterval< UnsignedByteType > image = ArrayImgs.unsignedBytes( dims );
		final RandomAccess< UnsignedByteType > imageAccess = image.randomAccess();
		for ( int y = 0; y < dims[ 1 ]; y++ )
			for ( int x = 0; x < dims[ 0 ]; x++ )
			{
				imageAccess.setPosition( new long[] { x, y } );
				imageAccess.get().set( x );
			}

		final URL transformResource = ElastixBSplineRealTransformSamplingTest.class
				.getResource( "/elastix/TransformParameters.BSpline2D.TranslationX.txt" );
		Assertions.assertNotNull( transformResource, "Missing Elastix BSpline transform test resource" );

		ElastixBSplineTransform elastixTransform = ( ElastixBSplineTransform ) ElastixTransform.load( new File( transformResource.toURI() ) );
		final RealTransform transform = ElastixBSplineToBSplineRealTransform.convert( elastixTransform );
		final double[] source = new double[ 2 ];
		final double[] target = new double[ 2 ];

		source[ 0 ] = 10;
		source[ 1 ] = 20;
		transform.apply( source, target );
		final int valueAt1020 = sampleNearest( image, target );

		source[ 0 ] = 30;
		source[ 1 ] = 5;
		transform.apply( source, target );
		final int valueAt305 = sampleNearest( image, target );

		// The 2D test transform encodes a +10 translation in x.
		Assertions.assertEquals( 20, valueAt1020 );
		Assertions.assertEquals( 40, valueAt305 );
	}

	private static int sampleNearest( final RandomAccessibleInterval< UnsignedByteType > image, final double[] position )
	{
		final long x = Math.round( position[ 0 ] );
		final long y = Math.round( position[ 1 ] );
		final RandomAccess< UnsignedByteType > ra = image.randomAccess();
		ra.setPosition( new long[] { x, y } );
		return ra.get().get();
	}
}
