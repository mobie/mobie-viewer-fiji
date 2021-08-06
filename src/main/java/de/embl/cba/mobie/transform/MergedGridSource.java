package de.embl.cba.mobie.transform;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;

public class MergedGridSource< T > implements Source< T >
{
	public MergedGridSource( List< Source< T > > gridSources, List< int[] > positions )
	{
		final Source< T > referenceSource = gridSources.get( 0 );
		final int numMipmapLevels = referenceSource.getNumMipmapLevels();

		final int[] minPos = new int[ 3 ];
		final int[] maxPos = new int[ 3 ];

		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] dimensions = new long[ 3 ];
			final long[] referenceSourceDimensions = referenceSource.getSource( 0, level ).dimensionsAsLongArray();

			for ( int d = 0; d < 3; d++ )
			{
				dimensions[ d ] = maxPos[ d ] - minPos[ d ] + 1;
				dimensions[ d ] *= referenceSourceDimensions[ d ];
			}

			new ReadOnlyCachedCellImgFactory().create(
					dimensions,
					nativeType,
					loader,
					ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
		}

	}

	@Override
	public boolean isPresent( int t )
	{
		return false;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		return null;
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return false;
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation interpolation )
	{
		return null;
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D affineTransform3D )
	{

	}

	@Override
	public T getType()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return null;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return null;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 0;
	}
}
