package org.embl.mobie.lib.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RealTransformedSourceTest
{
	@Test
	void getSourceReturnsRasterizedRealTransformAndAdjustedBounds()
	{
		final RandomAccessibleInterval< FloatType > image = ArrayImgs.floats( 10, 10, 10 );
		final Cursor< FloatType > cursor = Views.iterable( image ).localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.get().setReal( cursor.getDoublePosition( 0 ) );
		}

		final Source< FloatType > wrapped = new SimpleFloatSource( image );
		final RealTransform transform = new Translation3D( 1.0, 0.0, 0.0 );
		final RealTransformedSource< FloatType > transformed =
				new RealTransformedSource<>( wrapped, transform, "transformed" );

		final RandomAccessibleInterval< FloatType > wrappedInterval = wrapped.getSource( 0, 0 );
		final RandomAccessibleInterval< FloatType > transformedInterval = transformed.getSource( 0, 0 );

		boolean boundsDiffer = false;
		for ( int d = 0; d < 3; d++ )
		{
			if ( wrappedInterval.min( d ) != transformedInterval.min( d ) ||
					wrappedInterval.max( d ) != transformedInterval.max( d ) )
			{
				boundsDiffer = true;
				break;
			}
		}
		Assertions.assertTrue( boundsDiffer, "Expected transformed bounds to differ from wrapped source bounds." );

		final long[] point = new long[ 3 ];
		for ( int d = 0; d < 3; d++ )
			point[ d ] = Math.max( wrappedInterval.min( d ), transformedInterval.min( d ) );

		Assertions.assertTrue( contains( wrappedInterval, point ), "Point must be inside wrapped interval." );
		Assertions.assertTrue( contains( transformedInterval, point ), "Point must be inside transformed interval." );

		final double wrappedValue = valueAt( wrappedInterval, point );
		final double transformedValue = valueAt( transformedInterval, point );
		Assertions.assertNotEquals( wrappedValue, transformedValue, 1e-6,
				"Expected transformed raster to encode a different sampling of the wrapped source." );
	}

	private static double valueAt( final RandomAccessibleInterval< FloatType > interval, final long[] position )
	{
		final RandomAccess< FloatType > access = interval.randomAccess();
		access.setPosition( position );
		return access.get().getRealDouble();
	}

	private static boolean contains( final RandomAccessibleInterval< FloatType > interval, final long[] position )
	{
		for ( int d = 0; d < interval.numDimensions(); d++ )
		{
			if ( position[ d ] < interval.min( d ) || position[ d ] > interval.max( d ) )
				return false;
		}
		return true;
	}

	private static class SimpleFloatSource implements Source< FloatType >
	{
		private final RandomAccessibleInterval< FloatType > image;

		private SimpleFloatSource( final RandomAccessibleInterval< FloatType > image )
		{
			this.image = image;
		}

		@Override
		public boolean isPresent( final int t )
		{
			return true;
		}

		@Override
		public RandomAccessibleInterval< FloatType > getSource( final int t, final int level )
		{
			return image;
		}

		@Override
		public RealRandomAccessible< FloatType > getInterpolatedSource( final int t, final int level, final Interpolation method )
		{
			if ( method == Interpolation.NLINEAR )
				return Views.interpolate( Views.extendZero( image ), new NLinearInterpolatorFactory<>() );
			return Views.interpolate( Views.extendZero( image ), new NearestNeighborInterpolatorFactory<>() );
		}

		@Override
		public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
		{
			transform.identity();
		}

		@Override
		public FloatType getType()
		{
			return new FloatType();
		}

		@Override
		public String getName()
		{
			return "simple";
		}

		@Override
		public VoxelDimensions getVoxelDimensions()
		{
			return new FinalVoxelDimensions( "pixel", 1.0, 1.0, 1.0 );
		}

		@Override
		public int getNumMipmapLevels()
		{
			return 1;
		}

		@Override
		public boolean doBoundingBoxCulling()
		{
			return true;
		}
	}
}


