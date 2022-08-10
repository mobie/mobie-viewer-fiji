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
package org.embl.mobie.viewer.image;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.source.DefaultSourcePair;
import org.embl.mobie.viewer.source.MoBIEVolatileTypeMatcher;
import org.embl.mobie.viewer.source.RandomAccessibleIntervalMipmapSource;
import org.embl.mobie.viewer.source.SourcePair;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.embl.mobie.viewer.transform.image.ImageGridTransformer;
import org.embl.mobie.viewer.transform.image.InitialisedMetadataImage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StitchedImage< T extends Type< T >, V extends Volatile< T > & Type< V > > implements Image< T >
{
	protected final T type;
	protected final Source< T > referenceSource;
	protected final String name;
	protected final List< ? extends Image< T > > images;
	protected final List< int[] > positions;
	protected final double relativeCellMargin;
	protected int[][] tileDimensions;
	protected double[] tileRealDimensions;
	protected RealMaskRealInterval mask;
	protected int numMipmapLevels;
	protected double[][] downSamplingFactors;
	protected DefaultSourcePair< T > sourcePair;
	protected V volatileType;
	protected AffineTransform3D sourceTransform;
	protected double[][] mipmapScales;
	private long[] minPos;
	private long[] maxPos;

	public StitchedImage( List< ? extends Image< T > > images, @Nullable List< int[] > positions, String imageName, double relativeCellMargin, boolean transformImageTiles )
	{
		this.images = images;
		this.positions = positions == null ? TransformHelper.createGridPositions( images.size() ) : positions;
		this.relativeCellMargin = relativeCellMargin;
		this.referenceSource = images.iterator().next().getSourcePair().getSource();
		this.name = imageName;
		this.type = referenceSource.getType().createVariable();
		this.sourceTransform = new AffineTransform3D();
		referenceSource.getSourceTransform( 0, 0, sourceTransform );
		// TODO: make my own VolatileTypeMatcher including AnnotationType
		this.volatileType = ( V ) MoBIEVolatileTypeMatcher.getVolatileTypeForType( type );
		this.numMipmapLevels = referenceSource.getNumMipmapLevels();

		setMinMaxPos();
		configureDownSamplingAndTileDimensions();
		setTileRealDimensions( tileDimensions[ 0 ] );
		setRealMask( tileRealDimensions );
		createSourcePair();

		if ( transformImageTiles )
			transform( images );
	}

	private double[] computeTileMarginOffset( Image< ? > image, double[] tileRealDimensions )
	{
		final RealInterval realInterval = image.getMask();

		final double[] imageRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
			imageRealDimensions[ d ] = ( realInterval.realMax( d ) - realInterval.realMin( d ) );

		final double[] translationOffset = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			translationOffset[ d ] = 0.5 * ( tileRealDimensions[ d ] - imageRealDimensions[ d ] );

		return translationOffset;
	}

	// Transform all the images that are stitched to be
	// at the same location as they appear in the stitched image.
	// This is currently needed for image annotation displays
	// in order to know the location of the annotated images.
	protected void transform( List< ? extends Image< ? > > images )
	{
		final Image< ? > referenceImage = images.get( 0 );
		final double[] offset = computeTileMarginOffset( referenceImage, tileRealDimensions );

		final List< List< ? extends Image< ? > > > nestedImages = new ArrayList<>();
		for ( Image< ? > image : images )
		{
			final List< Image< ? > > imagesAtGridPosition = new ArrayList<>();

			// Only the image at the first grid position
			// (the reference image) may have been loaded.
			// Avoid loading of the other images by providing the metadata
			// of the reference image (note that for a StitchedImage all images
			// are required to have the same metadata).
			if ( image == referenceImage )
			{
				imagesAtGridPosition.add( referenceImage );
			}
			else
			{
				// The reason for doing this is not the translation,
				// but the fact that a RegionLabelImage may be build to
				// annotate those images and that would trigger loading of the
				// data.
				final InitialisedMetadataImage initialisedMetadataImage = new InitialisedMetadataImage( image, referenceImage.getMask() );
				imagesAtGridPosition.add( initialisedMetadataImage );
			}

			if ( image instanceof StitchedImage )
			{
				// Also transform the image tiles that are contained
				// in the stitched image.
				// Here, we don't need to use InitialisedMetadataImage again,
				// because those tiles are already InitialisedMetadataImages.
				final List< String > tileNames = ( ( StitchedImage< ?, ? > ) image ).getTileImages().stream().map( i -> i.getName() ).collect( Collectors.toList() );
				final Set< Image< ? > > stitchedImages = ImageStore.getImages( tileNames );
				for ( Image< ? > containedImage : stitchedImages )
				{
					if ( containedImage instanceof StitchedImage )
						throw new UnsupportedOperationException("Nested stitching of MergedGridTransformation is currently not supported.");

					imagesAtGridPosition.add( containedImage );
				}
			}

			nestedImages.add( imagesAtGridPosition );
		}

		new ImageGridTransformer().transform( nestedImages, null, positions, tileRealDimensions, false, offset );
	}


	public List< ? extends Image< T > > getTileImages()
	{
		return images;
	}

	protected void createSourcePair()
	{
		final RandomAccessSupplier randomAccessSupplier = new RandomAccessSupplier();

		// non-volatile
		//
		final List< RandomAccessibleInterval< T > > mipmapRAIs = createStitchedRAIs( randomAccessSupplier );

		final RandomAccessibleIntervalMipmapSource< T > source = new RandomAccessibleIntervalMipmapSource<>(
				mipmapRAIs,
				type,
				mipmapScales,
				referenceSource.getVoxelDimensions(),
				sourceTransform,
				name );

		// volatile
		//
		final List< RandomAccessibleInterval< V > > volatileMipMapRAIs = createVolatileStitchedRAIs( randomAccessSupplier );

		final RandomAccessibleIntervalMipmapSource< V > volatileSource = new RandomAccessibleIntervalMipmapSource<>(
				volatileMipMapRAIs,
				volatileType,
				mipmapScales,
				referenceSource.getVoxelDimensions(),
				sourceTransform,
				name );


		sourcePair = new DefaultSourcePair<>( source, volatileSource );
	}

	protected List< RandomAccessibleInterval< V > > createVolatileStitchedRAIs( RandomAccessSupplier randomAccessSupplier )
	{
		final List< RandomAccessibleInterval< V >> stitchedMipMapRAIs = new ArrayList<>();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final V background = volatileType.createVariable();
			background.setValid( true );
			final FunctionRandomAccessible< V > randomAccessible = new FunctionRandomAccessible( 3, new VolatileBiConsumerSupplier( randomAccessSupplier, level, background ), () -> volatileType.createVariable() );
			final IntervalView< V > rai = Views.interval( randomAccessible, getInterval( level ) );
			stitchedMipMapRAIs.add( rai );
		}

		return stitchedMipMapRAIs;
	}

	class VolatileBiConsumerSupplier implements Supplier< BiConsumer< Localizable, V > >
	{
		private final RandomAccessSupplier randomAccessSupplier;
		private final int level;
		private int[] tileDimension;
		private final V background;
		private HashMap< String, RandomAccess< V > > tileToRandomAccess;

		public VolatileBiConsumerSupplier( RandomAccessSupplier randomAccessSupplier, int level, V background )
		{
			this.randomAccessSupplier = randomAccessSupplier;
			this.level = level;
			this.tileDimension = tileDimensions[ level ];
			this.background = background;
			tileToRandomAccess = new HashMap<>();
		}

		@Override
		// This essentially returns a randomAccess.
		// Now, since this is a stitched image, it needs
		// to stitch the random accesses of the tiles.
		// Thus, it internally needs to hold onto several random accesses.

		public BiConsumer< Localizable, V > get()
		{
			return new BiConsumer< Localizable, V >()
			{
				@Override
				public void accept( Localizable location, V volatileOutput )
				{
					int x = location.getIntPosition( 0 );
					int y = location.getIntPosition( 1 );
					final int xTileIndex = x / tileDimension[ 0 ];
					final int yTileIndex = y / tileDimension[ 1 ];

					if ( ! randomAccessSupplier.exists( level, xTileIndex, yTileIndex ) )
					{
						volatileOutput.set( background );
						return;
					};

					final String tileKey = xTileIndex + "-" + yTileIndex;

					if ( tileToRandomAccess.containsKey( tileKey ) )
					{
						x = x - xTileIndex * tileDimension[ 0 ];
						y = y - yTileIndex * tileDimension[ 1 ];
						final V volatileType = tileToRandomAccess.get( tileKey ).setPositionAndGet(   x, y, location.getIntPosition( 2 ) );
						volatileOutput.set( volatileType );
						return;
					}

					// Random access not yet available
					//
					final Status status = randomAccessSupplier.status( level, xTileIndex, yTileIndex );
					if ( status.equals( Status.Closed ) )
					{
						new Thread( () -> randomAccessSupplier.open( level, xTileIndex, yTileIndex ) ).start();
					}
					else if ( status.equals( Status.Open ) )
					{
						tileToRandomAccess.put( tileKey, randomAccessSupplier.getVolatileRandomAccess( level, xTileIndex, yTileIndex ) );
					}

					volatileOutput.setValid( false );
				}
			};
		}
	}

	protected List< RandomAccessibleInterval< T > > createStitchedRAIs( RandomAccessSupplier randomAccessGrid )
	{
		final List< RandomAccessibleInterval< T >> stitchedRAIs = new ArrayList<>();
		for ( int l = 0; l < numMipmapLevels; l++ )
		{
			final int[] cellDimension = tileDimensions[ l ];
			final int level = l;
			BiConsumer< Localizable, T > biConsumer = ( location, value ) ->
			{
				int a = 1; // TODO;
//				int x = location.getIntPosition( 0 );
//				int y = location.getIntPosition( 1 );
//				final int xCellIndex = x / cellDimension[ 0 ];
//				final int yCellIndex = y / cellDimension[ 1 ];
//				// TODO: move this into the function computeTranslation
//				x = x - xCellIndex * cellDimension [ 0 ];
//				y = y - yCellIndex * cellDimension [ 1 ];
//
//				final RandomAccessSupplier< T > randomAccessSupplier = randomAccessGrid[ xCellIndex ][ yCellIndex ];
//
//				if ( randomAccessSupplier == null )
//				{
//					value.set( type );
//					return; // grid position is empty
//				}
//
//				// TODO
//				//value.set( randomAccessSupplier.get( level, x, y ) );
			};

			final FunctionRandomAccessible< T > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, () -> type.createVariable() );
			final IntervalView< T > rai = Views.interval( randomAccessible, getInterval( level ) );

			stitchedRAIs.add( rai );
		}

		return stitchedRAIs;
	}

	protected void setTileRealDimensions( int[] cellDimensions )
	{
		tileRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
			tileRealDimensions[ d ] = cellDimensions[ d ] * Affine3DHelpers.extractScale( sourceTransform, d );
	}

	protected void configureDownSamplingAndTileDimensions( )
	{
		final int numDimensions = referenceSource.getVoxelDimensions().numDimensions();

		final double[][] voxelSizes = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			referenceSource.getSourceTransform( 0, level, affineTransform3D );
			for ( int d = 0; d < numDimensions; d++ )
				voxelSizes[ level ][ d ] =
					Math.sqrt(
						affineTransform3D.get( 0, d ) * affineTransform3D.get( 0, d ) +
						affineTransform3D.get( 1, d ) * affineTransform3D.get( 1, d ) +	    				affineTransform3D.get( 2, d ) * affineTransform3D.get( 2, d )
						);
		}

		downSamplingFactors = new double[ numMipmapLevels ][ numDimensions ];
		mipmapScales = new double[ numMipmapLevels ][ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
		{
			downSamplingFactors[ 0 ][ d ] = 1.0;
			mipmapScales[ 0 ][ d ] = 1.0 / downSamplingFactors[ 0 ][ d ];
		}
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
			{
				downSamplingFactors[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ level - 1 ][ d ];
				mipmapScales[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ 0 ][ d ];
			}

		final double[] downSamplingFactorProducts = new double[ numDimensions ];
		Arrays.fill( downSamplingFactorProducts, 1.0D );

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downSamplingFactorProducts[ d ] *= downSamplingFactors[ level ][ d ];

		tileDimensions = new int[ numMipmapLevels ][ numDimensions ];

		// Adapt the cell dimensions such that they are divisible
		// by all relative changes of the resolutions between the different levels.
		// If we don't do this there are jumps of the images when zooming in and out;
		// i.e. the different resolution levels are rendered at slightly offset
		// positions.
		// TODO MUST
		//  shouldn't we adapt the downsampling factors and mipmaps accordingly??
		final RandomAccessibleInterval< T > source = referenceSource.getSource( 0, 0 );
		final long[] referenceSourceDimensions = source.dimensionsAsLongArray();
		tileDimensions[ 0 ] = MoBIEHelper.asInts( referenceSourceDimensions );
		for ( int d = 0; d < 2; d++ )
		{
			tileDimensions[ 0 ][ d ] *= ( 1 + 2.0 * relativeCellMargin );
			tileDimensions[ 0 ][ d ] = (int) ( downSamplingFactorProducts[ d ] * Math.ceil( tileDimensions[ 0 ][ d ] / downSamplingFactorProducts[ d ] ) );
		}

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				tileDimensions[ level ][ d ] = (int) ( tileDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );
	}

	protected FinalInterval getInterval( int level )
	{
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			min[ d ] = minPos[ d ] * tileDimensions[ level ][ d ];
			max[ d ] = ( maxPos[ d ] + 1 ) * tileDimensions[ level ][ d ];
		}
		return new FinalInterval( min, max );
	}

	protected void setRealMask( double[] tileRealDimensions )
	{
		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];

		for ( int d = 0; d < 2; d++ )
		{
			min[ d ] = minPos[ d ] * tileRealDimensions[ d ];
			max[ d ] = ( maxPos[ d ] + 1 ) * tileRealDimensions[ d ];
		}

		mask = GeomMasks.closedBox( min, max );
	}

	private void setMinMaxPos()
	{
		minPos = new long[ 3 ];
		maxPos = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			final int finalD = d;
			minPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().orElseThrow( NoSuchElementException::new );
			maxPos[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).max().orElseThrow( NoSuchElementException::new );
		}
	}

	protected static long[] computeTranslation( int[] cellDimensions, long[] dataDimensions )
	{
		final long[] translation = new long[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			// position of the cell + offset for margin
			// TODO: The rounding error may contribute to the
			//   slight misalignment of the resolution levels.
			//   We should compensate for this error when building the Source.
			translation[ d ] = (long) ( ( cellDimensions[ d ] - dataDimensions[ d ] ) / 2.0 );
		}
		return translation;
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
	public RealMaskRealInterval getMask()
	{
		return mask;
	}

	enum Status // for RandomAccessSupplier
	{
		Closed,
		Opening,
		Open;
	}

	class RandomAccessSupplier
	{
		protected Map< String, RandomAccessible< T > > keyToRandomAccessible;
		protected Map< String, RandomAccessible< V > > keyToVolatileRandomAccessible;
		protected Map< String, Status > keyToStatus;
		protected Map< String, Image< T > > keyToImage;

		public RandomAccessSupplier( )
		{
			keyToRandomAccessible = new ConcurrentHashMap<>();
			keyToStatus = new ConcurrentHashMap<>();

			for ( int gridIndex = 0; gridIndex < positions.size(); gridIndex++ )
			{
				final int[] position = positions.get( gridIndex );
				keyToImage.put( getKey( position[ 0 ], position[ 1 ] ), images.get( gridIndex ) );

				for ( int level = 0; level < numMipmapLevels; level++ )
					keyToStatus.put( getKey( level, position[ 0 ], position[ 1 ] ), Status.Closed );
			}
		}

		public RandomAccess< T > getRandomAccess( int level, int xTileIndex, int yTileIndex )
		{
			return keyToRandomAccessible.get( getKey( level, xTileIndex, yTileIndex ) ).randomAccess();
		}

		public RandomAccess< V > getVolatileRandomAccess( int level, int xTileIndex, int yTileIndex )
		{
			return keyToVolatileRandomAccessible.get( getKey( level, xTileIndex, yTileIndex ) ).randomAccess();
		}

		private String getKey( int level, long x, long y )
		{
			return level + "-" + x + "-" + y;
		}

		public Status status( int level, int xTileIndex, int yTileIndex )
		{
			return keyToStatus.get( getKey( level, xTileIndex, yTileIndex ) );
		}

		public boolean exists( int level, int xTileIndex, int yTileIndex )
		{
			return keyToStatus.containsKey( getKey( level, xTileIndex, yTileIndex ) );
		}

		public void open( int level, int xTileIndex, int yTileIndex )
		{
			final String key = getKey( level, xTileIndex, yTileIndex );

			if ( keyToStatus.get( key ).equals( Status.Open )
				|| keyToStatus.get( key ).equals( Status.Opening ) )
				return;

			keyToStatus.put( key, Status.Opening );

			int t = 0; // TODO

			// Open the source
			//
			final Image< T > image = keyToImage.get( getKey( xTileIndex, yTileIndex ) );
			final RandomAccessibleInterval< T > rai = image.getSourcePair().getSource().getSource( t, level );
			final RandomAccessibleInterval< ? extends Volatile< T > > vRai = image.getSourcePair().getVolatileSource().getSource( t, level );

			// Extend bounds to be able to
			// accommodate grid margin
			//
			RandomAccessible< T > randomAccessible = new ExtendedRandomAccessibleInterval( rai, new OutOfBoundsConstantValueFactory<>( type.createVariable() ) );
			RandomAccessible< V > vRandomAccessible = new ExtendedRandomAccessibleInterval( vRai, new OutOfBoundsConstantValueFactory<>( volatileType.createVariable() ) );

			// shift to create a margin
			//
			final long[] offset = computeTranslation( tileDimensions[ level ], rai.dimensionsAsLongArray() );
			final RandomAccessible< T > translateRa = Views.translate( randomAccessible, offset );
			final RandomAccessible< V > translateVRa = Views.translate( vRandomAccessible, offset );

			// register
			//
			keyToRandomAccessible.put( key, translateRa );
			keyToVolatileRandomAccessible.put( key, translateVRa );
			keyToStatus.put( key, Status.Open );
		}

		private String getKey( int xTileIndex, int yTileIndex )
		{
			return xTileIndex + "-" + yTileIndex;
		}
	}
}
