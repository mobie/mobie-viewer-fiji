package org.embl.mobie.lib.volume;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class FloodFill< T extends Type< T > >
{
	// input
	private final RandomAccessibleInterval< T > source;
	private final Shape shape;
	private final long maxRegionSize;

	// other
	private int n;
	private long[] min;
	private long[] max;

	public T getSeedValue()
	{
		return seedValue;
	}

	private T seedValue;
	private ArrayList< long[] > coordinates;
	private RandomAccessibleInterval< BitType > regionMask;

	// output
	private boolean maxRegionSizeReached;

	public FloodFill( RandomAccessibleInterval< T > source,
					  Shape shape,
					  long maxRegionSize )
	{
		this.source = source;
		this.shape = shape;
		this.maxRegionSize = maxRegionSize;
		n = source.numDimensions();
	}

	public void run( long[] seedCoordinate )
	{
		maxRegionSizeReached = false;

		setSeedValue( seedCoordinate );

		initCoordinates( seedCoordinate );

		initBoundingBox();

		floodFill();
	}

	public RandomAccessibleInterval< BitType > getCroppedRegionMask()
	{
		RandomAccessibleInterval< BitType > croppedMask = Views.interval( regionMask, new FinalInterval( min, max ) );

		return croppedMask;
	}

	public boolean isMaxRegionSizeReached()
	{
		return maxRegionSizeReached;
	}

	private void floodFill()
	{
		regionMask = new DiskCachedCellImgFactory<>( new BitType() ).create( source );
		regionMask = Views.translate( regionMask, Intervals.minAsLongArray( source ) ); // adjust offset
		final ExtendedRandomAccessibleInterval extendedRegionMask = Views.extendZero( regionMask ); // add oob strategy

		final T outOfBounds = source.randomAccess().get().createVariable();
		final RandomAccessible< Neighborhood< T > > neighborhood = shape.neighborhoodsRandomAccessible( Views.extendValue( source, outOfBounds ) );
		final RandomAccess< Neighborhood< T > > neighborhoodAccess = neighborhood.randomAccess();

		final RandomAccess< BitType > extendedMaskAccess = extendedRegionMask.randomAccess();

		for ( int i = 0; i < coordinates.size(); ++i )
		{
			if ( i > maxRegionSize )
			{
				maxRegionSizeReached = true;
				break;
			}

			neighborhoodAccess.setPosition( coordinates.get( i ) );

			final Cursor< T > neighborhoodCursor = neighborhoodAccess.get().cursor();

			while ( neighborhoodCursor.hasNext() )
			{
				neighborhoodCursor.next();

				if ( neighborhoodCursor.get().valueEquals( seedValue ) )
				{
					final long[] coordinate = new long[ n ];
					neighborhoodCursor.localize( coordinate );
					extendedMaskAccess.setPosition( coordinate );

					if ( !extendedMaskAccess.get().get() )
					{
						extendedMaskAccess.get().setOne();
						coordinates.add( coordinate );
						updateBoundingBox( coordinate );
					}
				}
			}
		}
	}

	private void initCoordinates( long[] seedCoordinate )
	{
		coordinates = new ArrayList<>();
		coordinates.add( seedCoordinate );
	}

	private void setSeedValue( long[] seed )
	{
		final RandomAccess< T > sourceAccess = source.randomAccess();
		sourceAccess.setPosition( seed );
		seedValue = sourceAccess.get();
	}

	private void initBoundingBox()
	{
		min = new long[ n ];
		max = new long[ n ];

		for ( int d = 0; d < min.length; ++d )
		{
			min[ d ] = Long.MAX_VALUE;
			max[ d ] = Long.MIN_VALUE;
		}
	}

	private void updateBoundingBox( long[] coordinate )
	{
		for ( int d = 0; d < min.length; ++d )
		{
			if ( coordinate[ d ] < min[ d ] ) min[ d ] = coordinate[ d ];
			if ( coordinate[ d ] > max[ d ] ) max[ d ] = coordinate[ d ];
		}
	}
}

