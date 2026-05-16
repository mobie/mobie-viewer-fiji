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
	void throwsFor2DElastixBSplineTransform() throws Exception
	{
		final URL transformResource = ElastixBSplineRealTransformSamplingTest.class
				.getResource( "/elastix/TransformParameters.BSpline2D.TranslationX.txt" );
		Assertions.assertNotNull( transformResource, "Missing Elastix BSpline transform test resource" );

		ElastixBSplineTransform elastixTransform = ( ElastixBSplineTransform ) ElastixTransform.load( new File( transformResource.toURI() ) );
		final IllegalArgumentException exception = Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> ElastixBSplineToBSplineRealTransform.convert( elastixTransform )
		);
		Assertions.assertTrue( exception.getMessage().contains( "Only 3D Elastix BSpline transforms" ) );
	}
}
