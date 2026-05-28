package org.embl.mobie.lib.transform;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

class DisplacementFieldTransformIOTest
{
	@Test
	void savesAndLoadsJsonRawDisplacementField() throws Exception
	{
		final long[] dims = new long[] { 3, 2, 2, 2 };
		final RandomAccessibleInterval< FloatType > interleaved = ArrayImgs.floats( dims );
		final RandomAccess< FloatType > access = interleaved.randomAccess();

		for ( int z = 0; z < 2; z++ )
			for ( int y = 0; y < 2; y++ )
				for ( int x = 0; x < 2; x++ )
				{
					access.setPosition( new long[] { 0, x, y, z } );
					access.get().set( 1f );
					access.setPosition( new long[] { 1, x, y, z } );
					access.get().set( 2f );
					access.setPosition( new long[] { 2, x, y, z } );
					access.get().set( 3f );
				}

		final File tempDir = Files.createTempDirectory( "mobie-dfield-io-test" ).toFile();
		final File jsonFile = new File( tempDir, "inverse-field.json" );

		DisplacementFieldTransformIO.save( interleaved, new double[] { 1, 1, 1 }, new double[] { 0, 0, 0 }, jsonFile );
		final DisplacementFieldTransform transform = DisplacementFieldTransformIO.load( jsonFile.getAbsolutePath() );

		final double[] sourceA = new double[] { 0, 0, 0 };
		final double[] targetA = new double[ 3 ];
		transform.apply( sourceA, targetA );
		Assertions.assertArrayEquals( new double[] { 1, 2, 3 }, targetA, 1e-6 );

		final double[] sourceB = new double[] { 1, 1, 1 };
		final double[] targetB = new double[ 3 ];
		transform.apply( sourceB, targetB );
		Assertions.assertArrayEquals( new double[] { 2, 3, 4 }, targetB, 1e-6 );
	}
}

