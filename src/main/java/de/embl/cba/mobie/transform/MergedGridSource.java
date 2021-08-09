package de.embl.cba.mobie.transform;

import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.mobie.Utils;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;

import java.util.List;
import java.util.NoSuchElementException;

public class MergedGridSource< T > implements Source< T >
{
	private final T type;
	private final Source< T > referenceSource;
	private final String mergedGridSourceName;

	public MergedGridSource( List< Source< T > > gridSources, List< int[] > positions, String mergedGridSourceName )
	{
		referenceSource = gridSources.get( 0 );
		this.mergedGridSourceName = mergedGridSourceName;
		type = Util.getTypeFromInterval( referenceSource.getSource( 0, 0 ) );

		final int[] minPos = new int[ 3 ];
		final int[] maxPos = new int[ 3 ];

		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}

		int numMipmapLevels = referenceSource.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] dimensions = new long[ 3 ];

			final long[] referenceSourceDimensions = referenceSource.getSource( 0, level ).dimensionsAsLongArray();

			final int[] cellDimensions = Utils.asInts( referenceSourceDimensions ); // TODO: add grid spacing

			for ( int d = 0; d < 3; d++ )
			{
				dimensions[ d ] = maxPos[ d ] - minPos[ d ] + 1;
				dimensions[ d ] *= cellDimensions[ d ];
			}

			//VolatileViews.wrapAsVolatile(  )

//			new ReadOnlyCachedCellImgFactory().create(
//					dimensions,
//					type,
//					loader,
//					ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions ) );
		}

	}



	@Override
	public boolean isPresent( int t )
	{
		return referenceSource.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		return null;
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return referenceSource.doBoundingBoxCulling();
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
		return type;
	}

	@Override
	public String getName()
	{
		return mergedGridSourceName;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return referenceSource.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return referenceSource.getNumMipmapLevels();
	}
}
