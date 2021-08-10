package de.embl.cba.mobie.transform;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.mobie.Utils;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class MergedGridSource< T extends NativeType< T > & NumericType< T > > implements Source< T >
{
	private final T type;
	private final Source< T > referenceSource;
	private final String mergedGridSourceName;
	private final List< RandomAccessibleInterval< T > > mergedRandomAccessibleIntervals;
	private final DefaultInterpolators< T > interpolators;
	private int currentTimepoint = 0;

	public MergedGridSource( List< Source< T > > gridSources, List< int[] > positions, String mergedGridSourceName )
	{
		interpolators = new DefaultInterpolators<>();
		referenceSource = gridSources.get( 0 );
		this.mergedGridSourceName = mergedGridSourceName;
		type = Util.getTypeFromInterval( referenceSource.getSource( 0, 0 ) );
		mergedRandomAccessibleIntervals = createMergedRandomAccessibleIntervals( gridSources, referenceSource, positions, type );
	}

	private < T extends NativeType< T > & NumericType< T > > List< RandomAccessibleInterval< T > > createMergedRandomAccessibleIntervals( List< Source< T > > gridSources, Source< T > referenceSource, List< int[] > positions, T type )
	{
		List< RandomAccessibleInterval< T >> mergedRandomAccessibleIntervals = new ArrayList<>();
		int numMipmapLevels = referenceSource.getNumMipmapLevels();
		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			// TODO: add grid spacing
			final int[] cellDimensions = getCellDimensions( referenceSource.getSource( 0, level ) );
			long[] dimensions = getDimensions( positions, cellDimensions );
			long[] min = getMin( positions, cellDimensions );

			final Map< String, Integer > cellKeyToSourceIndex = new HashMap<>();
			for ( int i = 0; i < positions.size(); i++ )
			{
				final int[] position = positions.get( i );
				final long[] cellMins = new long[ 3 ];
				for ( int d = 0; d < 2; d++ )
				{
					cellMins[ d ] = position[ d ] * cellDimensions[ d ];
				}
				String key = getCellKey( cellMins );
				cellKeyToSourceIndex.put( key, i );
				if ( mergedGridSourceName.equals( "plate_nuclei" ) )
				{
					int a = 1;
				}
			}

			final RandomAccessibleIntervalCellLoader< T > cellLoader = new RandomAccessibleIntervalCellLoader( gridSources, cellKeyToSourceIndex, level, min );

			final CachedCellImg< T, ? > cachedCellImg = new ReadOnlyCachedCellImgFactory().create(
					dimensions,
					type,
					cellLoader,
					ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions ) );

			final IntervalView< T > translate = Views.translate( cachedCellImg, min );
			mergedRandomAccessibleIntervals.add( translate );
		}

		return mergedRandomAccessibleIntervals;
	}

	private static long[] getDimensions( List< int[] > positions, int[] cellDimensions )
	{
		long[] dimensions = new long[ 3 ];
		final int[] minPos = new int[ 3 ];
		final int[] maxPos = new int[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}
		for ( int d = 0; d < 3; d++ )
		{
			dimensions[ d ] = maxPos[ d ] - minPos[ d ] + 1;
			dimensions[ d ] *= cellDimensions[ d ];
		}

		return dimensions;
	}

	private static long[] getMin( List< int[] > positions, int[] cellDimensions )
	{
		final long[] minPos = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			minPos[ d ] *= cellDimensions[ d ];
		}

		return minPos;
	}


	private static int[] getCellDimensions( RandomAccessibleInterval< ? > source )
	{
		final long[] referenceSourceDimensions = source.dimensionsAsLongArray();
		final int[] cellDimensions = Utils.asInts( referenceSourceDimensions );
		return cellDimensions;
	}

	private static String getCellKey( long[] cellMins )
	{
		String key = "_";
		for ( int d = 0; d < 2; d++ )
		{
			key += cellMins[ d ] + "_";
		}
		return key;
	}

	@Override
	public boolean isPresent( int t )
	{
		return referenceSource.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		if ( t != 0 )
		{
			throw new UnsupportedOperationException( "Multiple time points not yet implemented for merged grid source."); // TODO
		}
		return mergedRandomAccessibleIntervals.get( level );
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return referenceSource.doBoundingBoxCulling();
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return Views.interpolate( Views.extendZero( getSource( t, level ) ), interpolators.get( method ) );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D affineTransform3D )
	{
		referenceSource.getSourceTransform( t, level, affineTransform3D );
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

	class RandomAccessibleIntervalCellLoader< T extends NativeType< T > > implements CellLoader< T >
	{
		private final List< Source< T > > gridSources;
		private final Map< String, Integer > cellKeyToSourceIndex;
		private final int level;
		private final long[] cellMinOffset;

		public RandomAccessibleIntervalCellLoader( List< Source< T > > gridSources,  Map< String, Integer > cellKeyToSourceIndex, int level, long[] cellMinOffset )
		{
			this.gridSources = gridSources;
			this.cellKeyToSourceIndex = cellKeyToSourceIndex;
			this.level = level;
			this.cellMinOffset = cellMinOffset;
		}

		@Override
		public void load( SingleCellArrayImg< T, ? > cell ) throws Exception
		{
			final long[] min = new long[ 3 ];
			cell.min( min );
			for ( int d = 0; d < 3; d++ )
				min[ d ] += cellMinOffset[ d ];

			final String cellKey = getCellKey( min );
			if ( cellKeyToSourceIndex.containsKey( cellKey ) )
			{
				final RandomAccessibleInterval< T > randomAccessibleInterval = gridSources.get( cellKeyToSourceIndex.get( cellKey ) ).getSource( 0, level );
				Cursor< T > s = Views.flatIterable( randomAccessibleInterval ).cursor();
				Cursor< T > t = cell.cursor();
				while(s.hasNext()) {
					t.next().set(s.next());
				}
			}
			else
			{
				// leave black
			}
		}
	}
}
