/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer.source;

import bdv.util.Affine3DHelpers;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.MoBIEHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class StitchedImage< N extends NumericType< N >, V extends Volatile< N > & NumericType< V > > implements Image< N >
{
	private final N type;
	private final Source< N > referenceSource;
	private final String name;
	private final List< Image< N > > images;
	private final List< int[] > positions;
	private final double relativeCellMargin;
	private int[][] cellDimensions;
	private double[] cellRealDimensions;
	private FinalRealInterval imageBounds;
	private int numMipmapLevels;
	private double[][] downSamplingFactors;
	private DefaultSourcePair< N > sourcePair;
	private V volatileType;

	public StitchedImage( List< Image< N > > images, List< int[] > positions, String imageName, double relativeCellMargin )
	{
		this.images = images;
		this.positions = positions;
		this.relativeCellMargin = relativeCellMargin;
		this.referenceSource = images.iterator().next().getSourcePair().getSource();
		this.name = imageName;
		this.type = referenceSource.getType();
		this.volatileType = ( V ) VolatileTypeMatcher.getVolatileTypeForType( ( NativeType ) type );
		this.numMipmapLevels = referenceSource.getNumMipmapLevels();

		setCellAndSourceDimensions();
		createSourcePair();
	}

	public List< Image< N > > getStitchedImages()
	{
		return images;
	}

	private void setCellAndSourceDimensions()
	{
		setCellDimensions();
		setCellRealDimensions( cellDimensions[ 0 ] );
		setMask( positions, cellDimensions[ 0 ] );
	}

	private void createSourcePair()
	{
		// non-volatile
		//
		final RandomAccessSupplier[][] randomAccessGrid = randomAccessGrid( false );

		final List< RandomAccessibleInterval< N > > mipmapRAIs = createStitchedRAIs( randomAccessGrid );

		final RandomAccessibleIntervalMipmapSource< N > source = new RandomAccessibleIntervalMipmapSource<>(
				mipmapRAIs.toArray( new RandomAccessibleInterval[ 0 ] ),
				type,
				downSamplingFactors, // TODO: correct??
				referenceSource.getVoxelDimensions(),
				name );


		// volatile
		//
		final RandomAccessSupplier[][] volatileGrid = randomAccessGrid( true );

		final List< RandomAccessibleInterval< V > > volatileMipmapRAIs = createVolatileStitchedRAIs( volatileGrid );

		final RandomAccessibleIntervalMipmapSource< V > volatileSource = new RandomAccessibleIntervalMipmapSource<>(
				volatileMipmapRAIs.toArray( new RandomAccessibleInterval[ 0 ] ),
				volatileType,
				downSamplingFactors, // TODO: correct??
				referenceSource.getVoxelDimensions(),
				name );


		sourcePair = new DefaultSourcePair<>( source, volatileSource );
	}

	private List< RandomAccessibleInterval< V > > createVolatileStitchedRAIs( RandomAccessSupplier< V >[][] randomAccessGrid )
	{
		final List< RandomAccessibleInterval< V >> stitchedRAIs = new ArrayList<>();
		for ( int l = 0; l < numMipmapLevels; l++ )
		{
			final int[] cellDimension = cellDimensions[ l ];
			final int level = l;
			BiConsumer< Localizable, V > biConsumer = ( location, value ) ->
			{
				int x = location.getIntPosition( 0 );
				int y = location.getIntPosition( 1 );
				final int xCellIndex = x / cellDimension[ 0 ];
				final int yCellIndex = y / cellDimension[ 1 ];
				// TODO: move this into the function computeTranslation
				x = x - xCellIndex * cellDimension [ 0 ];
				y = y - yCellIndex * cellDimension [ 1 ];

				final RandomAccessSupplier< V > randomAccessSupplier = randomAccessGrid[ xCellIndex ][ yCellIndex ];

				if ( randomAccessSupplier == null )
					return; // grid position is empty

				final Status status = randomAccessSupplier.status( level );
				if ( status.equals( Status.Open ) )
				{
					final V numericType = randomAccessSupplier.get( level, x, y, location.getIntPosition( 2 ) );
					value.set( numericType );
					value.setValid( true );
				}
				else if ( status.equals( Status.Opening ) )
				{
					value.setValid( false );
				}
				else if ( status.equals( Status.Closed ) )
				{
					value.setValid( false );
					new Thread( () -> randomAccessSupplier.open( level ) ).start();
				}
			};

			final FunctionRandomAccessible< V > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, () -> volatileType.createVariable() );
			final long[] dimensions = getDimensions( positions, cellDimension );
			final IntervalView< V > rai = Views.interval( randomAccessible, new FinalInterval( dimensions ) );
			stitchedRAIs.add( rai );
		}

		return stitchedRAIs;
	}


	private List< RandomAccessibleInterval< N > > createStitchedRAIs( RandomAccessSupplier< N >[][] randomAccessGrid )
	{
		final List< RandomAccessibleInterval< N >> stitchedRAIs = new ArrayList<>();
		for ( int l = 0; l < numMipmapLevels; l++ )
		{
			final int[] cellDimension = cellDimensions[ l ];
			final int level = l;
			BiConsumer< Localizable, N > biConsumer = ( location, value ) ->
			{
				int x = location.getIntPosition( 0 );
				int y = location.getIntPosition( 1 );
				final int xCellIndex = x / cellDimension[ 0 ];
				final int yCellIndex = y / cellDimension[ 1 ];
				// TODO: move this into the function computeTranslation
				x = x - xCellIndex * cellDimension [ 0 ];
				y = y - yCellIndex * cellDimension [ 1 ];

				final RandomAccessSupplier< N > randomAccessSupplier = randomAccessGrid[ xCellIndex ][ yCellIndex ];

				if ( randomAccessSupplier == null )
				{
					return; // grid position is empty
				}

				value.set( randomAccessSupplier.get( level, x, y, location.getIntPosition( 2 ) ) );
			};

			final FunctionRandomAccessible< N > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, () -> type.createVariable() );
			final long[] dimensions = getDimensions( positions, cellDimension );
			final IntervalView< N > rai = Views.interval( randomAccessible, new FinalInterval( dimensions ) );
			stitchedRAIs.add( rai );
		}
		return stitchedRAIs;
	}

	private void setCellRealDimensions( int[] cellDimension )
	{
		final AffineTransform3D referenceTransform = new AffineTransform3D();
		referenceSource.getSourceTransform( 0, 0, referenceTransform );
		cellRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			cellRealDimensions[ d ] = cellDimension[ d ] * Affine3DHelpers.extractScale( referenceTransform, d );
		}
	}

	public FinalRealInterval getRealMask()
	{
		return imageBounds;
	}

	public double[] getCellRealDimensions()
	{
		return cellRealDimensions;
	}

	private void setCellDimensions( )
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

		downSamplingFactors = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downSamplingFactors[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ level - 1 ][ d ];

		final double[] downsamplingFactorProducts = new double[ numDimensions ];
		Arrays.fill( downsamplingFactorProducts, 1.0D );

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downsamplingFactorProducts[ d ] *= downSamplingFactors[ level ][ d ];

		cellDimensions = new int[ numMipmapLevels ][ numDimensions ];

		// Adapt the cell dimensions such that they are divisible
		// by all relative changes of the resolutions between the different levels.
		// If we don't do this there are jumps of the images when zooming in and out;
		// i.e. the different resolution levels are rendered at slightly offset
		// positions.
		final RandomAccessibleInterval< N > source = referenceSource.getSource( 0, 0 );
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
				cellDimensions[ level ][ d ] = (int) ( cellDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );
			}
	}

	private RandomAccessSupplier[][] randomAccessGrid( boolean asVolatile )
	{
		int[] maxPos = getMaxPositions();

		final RandomAccessSupplier[][] randomAccesses = new RandomAccessSupplier[ maxPos[ 0 ] + 1 ][ maxPos[ 1 ] + 1 ];
		for ( int positionIndex = 0; positionIndex < positions.size(); positionIndex++ )
		{
			final int[] position = positions.get( positionIndex );
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				if ( asVolatile )
				{
					final RandomAccessSupplier< V > randomAccessSupplier = new RandomAccessSupplier( images.get( positionIndex ).getSourcePair().getVolatileSource() );
					randomAccesses[ position[ 0 ] ][ position[ 1 ] ] = randomAccessSupplier;
				}
				else
				{
					final RandomAccessSupplier< N > randomAccessSupplier = new RandomAccessSupplier( images.get( positionIndex ).getSourcePair().getSource() );
					randomAccesses[ position[ 0 ] ][ position[ 1 ] ] = randomAccessSupplier;
				}
			}
		}
		return randomAccesses;
	}

	private int[] getMaxPositions()
	{
		int maxPos[] = new int[ 2 ];
		for ( int positionIndex = 0; positionIndex < positions.size(); positionIndex++ )
		{
			final int[] position = positions.get( positionIndex );
			for ( int d = 0; d < 2; d++ )
				if ( position[ d ] > maxPos[ d ] )
					maxPos[ d ] = position[ d ];
		}
		return maxPos;
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

	private void setMask( List< int[] > positions, int[] cellDimensions )
	{
		final long[] minPos = new long[ 3 ];
		final long[] maxPos = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}

		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];

		final AffineTransform3D referenceTransform = new AffineTransform3D();
		referenceSource.getSourceTransform( 0, 0, referenceTransform );

		for ( int d = 0; d < 2; d++ )
		{
			final double scale = Affine3DHelpers.extractScale( referenceTransform, d );
			min[ d ] = minPos[ d ] * cellDimensions[ d ] * scale;
			max[ d ] = ( maxPos[ d ] + 1 ) * cellDimensions[ d ] * scale;
		}

		imageBounds = new FinalRealInterval( min, max );
	}

	private static long[] computeTranslation( int[] cellDimensions, long[] dataDimensions )
	{
		final long[] translation = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			// position of the cell + offset for margin
			translation[ d ] = (long) ( ( cellDimensions[ d ] - dataDimensions[ d ] ) / 2.0 );
		}
		return translation;
	}

	private int[][] getCellDimensions()
	{
		return cellDimensions;
	}

	@Override
	public SourcePair< N > getSourcePair()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public RealInterval getBounds( int t )
	{
		return imageBounds;
	}

	enum Status
	{
		Closed,
		Opening,
		Open;
	}

	class RandomAccessSupplier< T extends NumericType< T > >
	{
		private final Source< T > source;
		private Map< Integer, RandomAccess< T > > levelToRandomAccess;
		private Map< Integer, RandomAccessible< T > > levelToRandomAccessible;
		private Map< Integer, Status > levelToStatus;

		public RandomAccessSupplier( Source< T > source )
		{
			this.source = source;
			levelToRandomAccess = new ConcurrentHashMap<>();
			levelToRandomAccessible = new ConcurrentHashMap<>();
			levelToStatus = new ConcurrentHashMap<>();
			for ( int level = 0; level < numMipmapLevels; level++ )
				levelToStatus.put( level, Status.Closed );
		}

		public T get( int level, int x, int y, int z )
		{
			final long l = System.currentTimeMillis();
			final T value = levelToRandomAccessible.get( level ).randomAccess().setPositionAndGet( x, y, z );
			final long millis = System.currentTimeMillis() - l;
			//System.out.println( "Value: " + millis ) ;
			return value;
		}

		public Status status( int level )
		{
			return levelToStatus.get( level );
		}

		public synchronized void open( int level )
		{
			if ( levelToRandomAccess.get( level ) != null )
				return;

			levelToStatus.put( level, Status.Opening );

			// TODO: t
			final long l = System.currentTimeMillis();
			final RandomAccessibleInterval< T > rai = source.getSource( 0, level );
			System.out.println( "Grid: Open " + source.getName() + ", level=" + level + ": " + ( System.currentTimeMillis() - l ) + " ms") ;
			RandomAccessible< T > ra = Views.extendZero( rai );
			final long[] offset = computeTranslation( cellDimensions[ level ], rai.dimensionsAsLongArray() );
			final RandomAccessible< T > translate = Views.translate( ra, offset );
			levelToRandomAccessible.put( level, translate );
			levelToRandomAccess.put( level, translate.randomAccess() );

			levelToStatus.put( level, Status.Open );
		}
	}
}
