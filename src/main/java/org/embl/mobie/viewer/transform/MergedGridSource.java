package org.embl.mobie.viewer.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccess;
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
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.source.LabelSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class MergedGridSource< T extends NativeType< T > & NumericType< T > > implements Source< T >
{
	private final T type;
	private final Source< T > referenceSource;
	private final String mergedGridSourceName;
	private final List< RandomAccessibleInterval< T > > mergedRandomAccessibleIntervals;
	private final DefaultInterpolators< T > interpolators;
	private final List< Source< T > > gridSources;
	private final List< int[] > positions;
	private final double relativeCellMargin;
	private final boolean encodeSource;
	private int currentTimepoint = 0;
	private Map< String, long[] > sourceNameToVoxelTranslation;
	private int[][] cellDimensions;
	private double[] cellRealDimensions;
	private Set< SourceAndConverter > containedSourceAndConverters;

	public MergedGridSource( List< Source< T > > gridSources, List< int[] > positions, String mergedGridSourceName, double relativeCellMargin, boolean encodeSource )
	{
		this.gridSources = gridSources;
		this.positions = positions;
		this.relativeCellMargin = relativeCellMargin;
		this.encodeSource = encodeSource;
		this.interpolators = new DefaultInterpolators<>();
		this.referenceSource = gridSources.get( 0 );
		this.mergedGridSourceName = mergedGridSourceName;
		this.type = Util.getTypeFromInterval( referenceSource.getSource( 0, 0 ) );

		mergedRandomAccessibleIntervals = createMergedRandomAccessibleIntervals();
	}

	public static boolean instanceOf( SourceAndConverter< ? > sourceAndConverter )
	{
		final Source< ? > source = sourceAndConverter.getSpimSource();
		return instanceOf( source );
	}

	public static boolean instanceOf( Source< ? > source )
	{
		if ( source instanceof LabelSource )  source = ( ( LabelSource ) source ).getWrappedSource();

		final Source wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

		return wrappedSource instanceof MergedGridSource || source instanceof MergedGridSource;
	}

	public List< Source< T > > getGridSources()
	{
		return gridSources;
	}

	private List< RandomAccessibleInterval< T > > createMergedRandomAccessibleIntervals()
	{
		final List< RandomAccessibleInterval< T >> mergedRandomAccessibleIntervals = new ArrayList<>();
		final int numMipmapLevels = referenceSource.getNumMipmapLevels();
		cellDimensions = computeCellDimensions( numMipmapLevels );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		final FinalInterval interval = createInterval();
		final FinalRealInterval bounds = affineTransform3D.estimateBounds( interval );
		cellRealDimensions = bounds.maxAsDoubleArray();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] mergedDimensions = getDimensions( positions, cellDimensions[ level ] );

			final Map< String, Source< T > > cellKeyToSource = createCellKeyToSource( cellDimensions[ level ] );

			if ( level == 0 )
			{
				sourceNameToVoxelTranslation = createSourceNameToTranslation( cellDimensions[ level ], gridSources.get( 0 ).getSource( 0, 0 ).dimensionsAsLongArray() );
			}

			final RandomAccessibleIntervalCellLoader< T > cellLoader = new RandomAccessibleIntervalCellLoader( cellKeyToSource, level );

			final CachedCellImg< T, ? > cachedCellImg =
					new ReadOnlyCachedCellImgFactory().create(
						mergedDimensions,
						type,
						cellLoader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions[ level ] ) );


			mergedRandomAccessibleIntervals.add( cachedCellImg );
		}

		return mergedRandomAccessibleIntervals;
	}

	private FinalInterval createInterval()
	{
		final long[] max = MoBIEHelper.asLongs( cellDimensions[ 0 ] );
		for ( int d = 0; d < max.length; d++ )
			max[ d ] -= 1;
		final long[] min = new long[ 3 ];
		final FinalInterval interval = new FinalInterval( min, max );
		return interval;
	}

	public double[] getCellRealDimensions()
	{
		return cellRealDimensions;
	}

	private int[][] computeCellDimensions( int numMipmapLevels )
	{
		final int numDimensions = referenceSource.getVoxelDimensions().numDimensions();

		final AffineTransform3D at3D = new AffineTransform3D();
		referenceSource.getSourceTransform( 0, 0, at3D );

		final double[][] voxelSizes = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			referenceSource.getSourceTransform( 0, level, at3D );
			for ( int d = 0; d < numDimensions; d++ )
				voxelSizes[ level ][ d ] =
						Math.sqrt(
								at3D.get( 0, d ) * at3D.get( 0, d ) +
								at3D.get( 1, d ) * at3D.get( 1, d ) +
								at3D.get( 2, d ) * at3D.get( 2, d )
						);
		}

		double[][] downsamplingFactors = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downsamplingFactors[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ level - 1 ][ d ];

		final double[] downsamplingFactorProducts = new double[ numDimensions ];
		Arrays.fill( downsamplingFactorProducts, 1.0D );

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downsamplingFactorProducts[ d ] *= downsamplingFactors[ level ][ d ];

		int[][] cellDimensions = new int[ numMipmapLevels ][ numDimensions ];

		// Adapt the cell dimensions such that they are divisible
		// by all relative changes of the resolutions between the different levels.
		// If we don't do this there are jumps of the images when zooming in and out;
		// i.e. the different resolution levels are rendered at slightly offset
		// positions.
		final RandomAccessibleInterval< T > source = referenceSource.getSource( 0, 0 );
		final long[] referenceSourceDimensions = source.dimensionsAsLongArray();
		cellDimensions[ 0 ] = MoBIEHelper.asInts( referenceSourceDimensions );
		for ( int d = 0; d < 2; d++ )
		{
			cellDimensions[ 0 ][ d ] *= ( 1 + 2.0 * relativeCellMargin );
			cellDimensions[ 0 ][ d ] = (int) ( downsamplingFactorProducts[ d ] * Math.ceil( cellDimensions[ 0 ][ d ] / downsamplingFactorProducts[ d ] ) );
		}

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
			{
				cellDimensions[ level ][ d ] = (int) ( cellDimensions[ level - 1 ][ d ] / downsamplingFactors[ level ][ d ] );
			}

		return cellDimensions;
	}

	private Map< String, Source< T > > createCellKeyToSource( int[] cellDimensions )
	{
		final Map< String, Source< T > > cellKeyToSource = new HashMap<>();
		for ( int positionIndex = 0; positionIndex < positions.size(); positionIndex++ )
		{
			final int[] position = positions.get( positionIndex );
			final long[] cellMins = new long[ 3 ];
			for ( int d = 0; d < 2; d++ )
				cellMins[ d ] = position[ d ] * cellDimensions[ d ];

			String key = getCellKey( cellMins );
			cellKeyToSource.put( key, gridSources.get( positionIndex ) );

		}
		return cellKeyToSource;
	}

	private HashMap< String, long[] > createSourceNameToTranslation( int[] cellDimensions, long[] dataDimensions )
	{
		final HashMap< String, long[] > sourceNameToTranslation = new HashMap<>();

		for ( int positionIndex = 0; positionIndex < positions.size(); positionIndex++ )
		{
			final int[] position = positions.get( positionIndex );
			final long[] cellMin = new long[ 3 ];
			for ( int d = 0; d < 2; d++ )
				cellMin[ d ] = position[ d ] * cellDimensions[ d ];

			final long[] translation = computeTranslation( cellDimensions, cellMin, dataDimensions );
			sourceNameToTranslation.put( gridSources.get( positionIndex ).getName(), translation );
		}

		return sourceNameToTranslation;
	}

	private static long[] getDimensions( List< int[] > positions, int[] cellDimensions )
	{
		long[] dimensions = new long[ 3 ];
		final int[] maxPos = new int[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}

		for ( int d = 0; d < 3; d++ )
			dimensions[ d ] = ( maxPos[ d ] + 1 ) * cellDimensions[ d ];

		return dimensions;
	}

	private static String getCellKey( long[] cellMins )
	{
		String key = "_";
		for ( int d = 0; d < 2; d++ )
			key += cellMins[ d ] + "_";

		return key;
	}

	private long[] computeTranslation( int[] cellDimensions, long[] cellMin, long[] dataDimensions )
	{
		final long[] translation = new long[ cellMin.length ];
		for ( int d = 0; d < 2; d++ )
		{
			// position of the cell + offset for margin
			translation[ d ] = cellMin[ d ] + (long) ( ( cellDimensions[ d ] - dataDimensions[ d ] ) / 2.0 );
		}
		return translation;
	}

	// TODO: not used
	public Map< String, long[] > getSourceNameToVoxelTranslation()
	{
		return sourceNameToVoxelTranslation;
	}

	public int[][] getCellDimensions()
	{
		return cellDimensions;
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

	public void setContainedSourceAndConverters( Set< SourceAndConverter > containedSourceAndConverters )
	{
		this.containedSourceAndConverters = containedSourceAndConverters;
	}

	public Set< SourceAndConverter > getContainedSourceAndConverters()
	{
		return containedSourceAndConverters;
	}

	class RandomAccessibleIntervalCellLoader< T extends NativeType< T > > implements CellLoader< T >
	{
		private final Map< String, Source< T > > cellKeyToSource;
		private final int level;

		public RandomAccessibleIntervalCellLoader( Map< String, Source< T > > cellKeyToSource, int level )
		{
			this.cellKeyToSource = cellKeyToSource;
			this.level = level;
		}

		@Override
		public void load( SingleCellArrayImg< T, ? > cell ) throws Exception
		{
			final String cellKey = getCellKey( cell.minAsLongArray() );

			if ( ! cellKeyToSource.containsKey( cellKey ) )
			{
				return;
			}
			else
			{
				// Get the RAI for this cell
				final Source< T > source = cellKeyToSource.get( cellKey );
				RandomAccessibleInterval< T > data = source.getSource( currentTimepoint, level );

				// Create a view that is shifted to the cell position
				final long[] offset = computeTranslation( MoBIEHelper.asInts( cell.dimensionsAsLongArray() ), cell.minAsLongArray(), data.dimensionsAsLongArray() );
				data = Views.translate( Views.zeroMin( data ), offset );

				// copy RAI into cell
				Cursor< T > sourceCursor = Views.iterable( data ).cursor();
				RandomAccess< T > targetAccess = cell.randomAccess();

				if ( encodeSource )
				{
					final String name = source.getName();
					while ( sourceCursor.hasNext() )
					{
						sourceCursor.fwd();
						// copy the sourceCursor in order not to modify it by the source name encoding
						targetAccess.setPositionAndGet( sourceCursor ).set( sourceCursor.get().copy() );
						SourceNameEncoder.encodeName( ( UnsignedIntType ) targetAccess.get(), name );
					}
				}
				else
				{
					while ( sourceCursor.hasNext() )
					{
						sourceCursor.fwd();
						targetAccess.setPositionAndGet( sourceCursor ).set( sourceCursor.get() );
					}
				}
			}
		}
	}
}
