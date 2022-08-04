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
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.embl.mobie.viewer.transform.TransformedGridTransformation;
import org.embl.mobie.viewer.transform.image.InitialisedBoundsImage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class StitchedImage< T extends Type< T >, V extends Volatile< T > & Type< V > > implements Image< T >
{
	protected final T type;
	protected final Source< T > referenceSource;
	protected final String name;
	protected final List< Image< T > > images;
	protected final List< int[] > positions;
	protected final double relativeCellMargin;
	protected int[][] cellDimensions;
	protected double[] cellRealDimensions;
	protected FinalRealInterval imageBounds;
	protected int numMipmapLevels;
	protected double[][] downSamplingFactors;
	protected DefaultSourcePair< T > sourcePair;
	protected V volatileType;
	protected AffineTransform3D sourceTransform;

	public StitchedImage( List< Image< T > > images, @Nullable List< int[] > positions, String imageName, double relativeCellMargin, boolean transformImages )
	{
		this.images = images;
		this.positions = positions == null ? TransformHelper.createGridPositions( images.size() ) : positions;
		this.relativeCellMargin = relativeCellMargin;
		try
		{
			this.referenceSource = images.iterator().next().getSourcePair().getSource();
		} catch ( Exception e )
		{
			throw ( e );
		}
		this.name = imageName;
		this.type = referenceSource.getType();
		this.sourceTransform = new AffineTransform3D();
		referenceSource.getSourceTransform( 0, 0, sourceTransform );
		// TODO: make my own VolatileTypeMatcher including AnnotationType
		this.volatileType = ( V ) MoBIEVolatileTypeMatcher.getVolatileTypeForType( type );
		this.numMipmapLevels = referenceSource.getNumMipmapLevels();

		setCellAndSourceDimensions();
		createSourcePair();

		if ( transformImages )
			transform( images );
	}

	// Transform all the images that are stitched to be
	// at the same location as they appear in the stitched image.
	// This is currently needed for image annotation displays
	// in order to know the location of the annotated images.
	protected void transform( List< Image< T > > images )
	{
		final Image< T > referenceImage = images.get( 0 );

		final TransformedGridTransformation< T > gridTransformation = new TransformedGridTransformation<>();
		gridTransformation.positions = positions;
		final ArrayList< List< Image< T > > > nestedImages = new ArrayList<>();
		for ( Image< T > image : images )
		{
			final ArrayList< Image< T > > imagesAtGridPosition = new ArrayList<>();

			if ( image instanceof StitchedImage )
			{
				// Transform the images that are contained
				// in the stitched image.
				final List< String > stitchedImageNames = ( ( StitchedImage< ?, ? > ) image ).getTileImages().stream().map( i -> i.getName() ).collect( Collectors.toList() );
				final Set< Image< ? > > stitchedImages = ImageStore.getImages( stitchedImageNames );
				for ( Image< ? > containedImage : stitchedImages )
				{
					if ( containedImage instanceof StitchedImage )
						throw new UnsupportedOperationException("Nested stitching of MergedGridTransformation is currently not supported.");

					imagesAtGridPosition.add( ( Image< T > ) containedImage );
				}
			}
			else
			{
				// Use bounds of reference image.
				// This avoids loading of the actual image
				// when another method is asking the transformed
				// image for its bounds.
				final InitialisedBoundsImage initialisedBoundsImage = new InitialisedBoundsImage( image, referenceImage.getBounds( 0 ) );
				imagesAtGridPosition.add( initialisedBoundsImage );
			}
			nestedImages.add( imagesAtGridPosition );
		}

		// TODO!
		//final List< Image< N > > transformed = gridTransformation.apply( nestedImages );
		//ImageStore.putImages( transformed );
	}


	public List< Image< T > > getTileImages()
	{
		return images;
	}

	protected void setCellAndSourceDimensions()
	{
		setCellDimensions();
		setCellRealDimensions( cellDimensions[ 0 ] );
		setMask( positions, cellDimensions[ 0 ] );
	}

	protected void createSourcePair()
	{
		// non-volatile
		//
		final RandomAccessSupplier< T >[][] randomAccessGrid = createRandomAccessGrid( type );

		final List< RandomAccessibleInterval< T > > mipmapRAIs = createStitchedRAIs( randomAccessGrid );

		// TODO: Create own one for non-numeric types
		final RandomAccessibleIntervalMipmapSource< T > source = new RandomAccessibleIntervalMipmapSource<>(
				mipmapRAIs,
				type,
				downSamplingFactors, // TODO: correct??
				referenceSource.getVoxelDimensions(),
				sourceTransform,
				name );

		// volatile
		//
		final RandomAccessSupplier< V >[][] volatileGrid = createRandomAccessGrid( volatileType );

		final List< RandomAccessibleInterval< V > > volatileMipmapRAIs = createVolatileStitchedRAIs( volatileGrid );

		// TODO: Create own one for non-numeric types
		final RandomAccessibleIntervalMipmapSource< V > volatileSource = new RandomAccessibleIntervalMipmapSource<>(
				volatileMipmapRAIs,
				volatileType,
				downSamplingFactors, // TODO: correct??
				referenceSource.getVoxelDimensions(),
				sourceTransform,
				name );


		sourcePair = new DefaultSourcePair<>( source, volatileSource );
	}

	protected List< RandomAccessibleInterval< V > > createVolatileStitchedRAIs( RandomAccessSupplier< V >[][] randomAccessGrid )
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


	protected List< RandomAccessibleInterval< T > > createStitchedRAIs( RandomAccessSupplier< T >[][] randomAccessGrid )
	{
		final List< RandomAccessibleInterval< T >> stitchedRAIs = new ArrayList<>();
		for ( int l = 0; l < numMipmapLevels; l++ )
		{
			final int[] cellDimension = cellDimensions[ l ];
			final int level = l;
			BiConsumer< Localizable, T > biConsumer = ( location, value ) ->
			{
				int x = location.getIntPosition( 0 );
				int y = location.getIntPosition( 1 );
				final int xCellIndex = x / cellDimension[ 0 ];
				final int yCellIndex = y / cellDimension[ 1 ];
				// TODO: move this into the function computeTranslation
				x = x - xCellIndex * cellDimension [ 0 ];
				y = y - yCellIndex * cellDimension [ 1 ];

				final RandomAccessSupplier< T > randomAccessSupplier = randomAccessGrid[ xCellIndex ][ yCellIndex ];

				if ( randomAccessSupplier == null )
				{
					return; // grid position is empty
				}

				value.set( randomAccessSupplier.get( level, x, y, location.getIntPosition( 2 ) ) );
			};

			final FunctionRandomAccessible< T > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, () -> type.createVariable() );
			final long[] dimensions = getDimensions( positions, cellDimension );
			final IntervalView< T > rai = Views.interval( randomAccessible, new FinalInterval( dimensions ) );
			stitchedRAIs.add( rai );
		}
		return stitchedRAIs;
	}

	protected void setCellRealDimensions( int[] cellDimension )
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

	protected void setCellDimensions( )
	{
		final int numDimensions = referenceSource.getVoxelDimensions().numDimensions();

		final double[][] voxelSizes = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			referenceSource.getSourceTransform( 0, level, sourceTransform );
			for ( int d = 0; d < numDimensions; d++ )
				voxelSizes[ level ][ d ] =
						Math.sqrt(
								sourceTransform.get( 0, d ) * sourceTransform.get( 0, d ) +
										sourceTransform.get( 1, d ) * sourceTransform.get( 1, d ) +
										sourceTransform.get( 2, d ) * sourceTransform.get( 2, d )
						);
		}

		downSamplingFactors = new double[ numMipmapLevels ][ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
			downSamplingFactors[ 0 ][ d ] = 1.0;
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downSamplingFactors[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ level - 1 ][ d ];

		final double[] downSamplingFactorProducts = new double[ numDimensions ];
		Arrays.fill( downSamplingFactorProducts, 1.0D );

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downSamplingFactorProducts[ d ] *= downSamplingFactors[ level ][ d ];

		cellDimensions = new int[ numMipmapLevels ][ numDimensions ];

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
			cellDimensions[ 0 ][ d ] = (int) ( downSamplingFactorProducts[ d ] * Math.ceil( cellDimensions[ 0 ][ d ] / downSamplingFactorProducts[ d ] ) );
		}

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
			{
				cellDimensions[ level ][ d ] = (int) ( cellDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );
			}
	}

	protected< T extends Type< T > > RandomAccessSupplier< T >[][] createRandomAccessGrid( T type )
	{
		int[] maxPos = getMaxPositions();

		final RandomAccessSupplier< T >[][] randomAccesses = new RandomAccessSupplier[ maxPos[ 0 ] + 1 ][ maxPos[ 1 ] + 1 ];
		for ( int positionIndex = 0; positionIndex < positions.size(); positionIndex++ )
		{
			final int[] position = positions.get( positionIndex );
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				final RandomAccessSupplier< T > randomAccessSupplier = new RandomAccessSupplier( images.get( positionIndex ), type );
				randomAccesses[ position[ 0 ] ][ position[ 1 ] ] = randomAccessSupplier;
			}
		}
		return randomAccesses;
	}

	protected int[] getMaxPositions()
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

	protected static long[] getDimensions( List< int[] > positions, int[] cellDimensions )
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

	protected void setMask( List< int[] > positions, int[] cellDimensions )
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

	protected static long[] computeTranslation( int[] cellDimensions, long[] dataDimensions )
	{
		final long[] translation = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			// position of the cell + offset for margin
			translation[ d ] = (long) ( ( cellDimensions[ d ] - dataDimensions[ d ] ) / 2.0 );
		}
		return translation;
	}

	protected int[][] getCellDimensions()
	{
		return cellDimensions;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		return sourcePair;
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

	enum Status // for the RandomAccessSupplier
	{
		Closed,
		Opening,
		Open;
	}

	class RandomAccessSupplier< T extends Type< T > >
	{
		protected final Image< T > image;
		protected final T type;
		protected Map< Integer, RandomAccess< T > > levelToRandomAccess;
		protected Map< Integer, RandomAccessible< T > > levelToRandomAccessible;
		protected Map< Integer, Status > levelToStatus;

		public RandomAccessSupplier( Image< T > image, T type )
		{
			this.image = image;
			this.type = type;
			levelToRandomAccess = new ConcurrentHashMap<>();
			levelToRandomAccessible = new ConcurrentHashMap<>();
			levelToStatus = new ConcurrentHashMap<>();
			for ( int level = 0; level < numMipmapLevels; level++ )
				levelToStatus.put( level, Status.Closed );
		}

		public T get( int level, int x, int y, int z )
		{
			final T value = levelToRandomAccessible.get( level ).randomAccess().setPositionAndGet( x, y, z );
			return value;
		}

		public Status status( int level )
		{
			return levelToStatus.get( level );
		}

		public synchronized void open( int level )
		{
			int t = 0; // TODO

			if ( levelToRandomAccess.get( level ) != null )
				return;

			levelToStatus.put( level, Status.Opening );

			final long l = System.currentTimeMillis();
			RandomAccessibleInterval< T > rai;
			if ( type instanceof Volatile )
				rai = ( RandomAccessibleInterval< T > ) image.getSourcePair().getVolatileSource().getSource( t, level );
			else
				rai = ( RandomAccessibleInterval< T > ) image.getSourcePair().getSource().getSource( t, level );

			System.out.println( "Grid: Open " + image.getName() + ", level=" + level + ": " + ( System.currentTimeMillis() - l ) + " ms") ;
			// TODO: I need my own extension
			final T outOfBoundsVariable = type.createVariable();
			RandomAccessible< T > ra = new ExtendedRandomAccessibleInterval<>( rai, new OutOfBoundsConstantValueFactory<>( outOfBoundsVariable ) );
			final long[] offset = computeTranslation( cellDimensions[ level ], rai.dimensionsAsLongArray() );
			final RandomAccessible< T > translate = Views.translate( ra, offset );
			levelToRandomAccessible.put( level, translate );
			levelToRandomAccess.put( level, translate.randomAccess() );
			levelToStatus.put( level, Status.Open );
		}
	}
}
