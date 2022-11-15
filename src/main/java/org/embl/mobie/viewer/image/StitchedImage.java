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

import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
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
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.ThreadHelper;
import org.embl.mobie.viewer.source.MoBIEVolatileTypeMatcher;
import org.embl.mobie.viewer.source.RandomAccessibleIntervalMipmapSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.source.SourcePair;
import org.embl.mobie.viewer.transform.image.ImageTransformer;
import org.embl.mobie.viewer.transform.TransformHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StitchedImage< T extends Type< T >, V extends Volatile< T > & Type< V > > implements Image< T >
{
	private final T type;
	private final String name;
	private List< ? extends Image< T > > images;
	private final List< int[] > positions;
	private final double relativeCellMargin;
	private int[][] tileDimensions;
	private double[][] downSamplingFactors;
	private double[][] mipmapScales;
	private double[][] marginTranslations;
	private HashMap< Integer, AffineTransform3D > levelToSourceTransform;
	private HashMap< Integer, long[] > levelToSourceDimensions;
	private double[] tileRealDimensions;
	private RealMaskRealInterval mask;
	private int numMipmapLevels;
	private DefaultSourcePair< T > sourcePair;
	private V volatileType;
	private long[] minPos;
	private long[] maxPos;
	private int numDimensions;
	private VoxelDimensions voxelDimensions;
	private TransformedSource< T > transformedSource;
	private RealMaskRealInterval referenceMask;

	private static AtomicInteger valueSupplierIndex = new AtomicInteger( 0 );
	private boolean debug = false;
	private AffineTransform3D sourceTransform;

	public StitchedImage( List< ? extends Image< T > > images, Image< T > metadataImage, @Nullable List< int[] > positions, String name, double relativeCellMargin )
	{
		this.images = images;

		// Fetch image dimensions, type and mask from metadataImage.
		// The metadataImage does not need to be part of the StitchedImage;
		// in fact, this is the whole point, as we can avoid loading
		// any of the images of the StitchedImage at this point.
		// This can make initialisation much faster.
		// This is useful if there are many StitchedImages to be initialised
		// such as the wells of an HTM screen.
		Source< T > metadataSource = metadataImage.getSourcePair().getSource();
		this.type = metadataSource.getType().createVariable();
		this.volatileType = ( V ) MoBIEVolatileTypeMatcher.getVolatileTypeForType( type );
		this.numMipmapLevels = metadataSource.getNumMipmapLevels();
		this.numDimensions = metadataSource.getVoxelDimensions().numDimensions();
		this.voxelDimensions = metadataSource.getVoxelDimensions();
		this.levelToSourceTransform = new HashMap<>();
		this.levelToSourceDimensions = new HashMap<>();

		// Get the reference mask from the metadata image.
		// We only need the spatial extent of the image.
		// The absolute position will be computed based on the tile
		// position. Thus, we remove current spatial offset that this
		// image may have already.
		this.referenceMask = GeomMasks.closedBox( metadataImage.getMask().minAsDoubleArray(), metadataImage.getMask().maxAsDoubleArray() );
		final AffineTransform3D translateToZeroInXY = new AffineTransform3D();
		final double[] translationVector = metadataImage.getMask().minAsDoubleArray();
		translationVector[ 2 ] = 0; // Don't change the position along the z-axis
		translateToZeroInXY.translate( translationVector );
		referenceMask = referenceMask.transform( translateToZeroInXY );

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			metadataSource.getSourceTransform( 0, level, affineTransform3D );
			final AffineTransform3D copy = affineTransform3D.copy();
			final long[] dimensions = metadataSource.getSource( 0, level ).dimensionsAsLongArray();
			copy.setTranslation( 0, 0, 0 );

			if ( debug )
			{
				System.out.println( "StitchedImage: " + name );
				System.out.println( "StitchedImage: Metadata source: " + metadataSource.getName() );
				System.out.println( "StitchedImage: Metadata transform: " + copy );
				System.out.println( "StitchedImage: Metadata dimensions: " + Arrays.toString( dimensions ) );
				System.out.println( "StitchedImage: Metadata tile mask: " + TransformHelper.maskToString( referenceMask ) );
			}
			levelToSourceTransform.put( level, copy );
			levelToSourceDimensions.put( level, dimensions );
		}

		this.sourceTransform = levelToSourceTransform.get( 0 );
		this.positions = positions == null ? TransformHelper.createGridPositions( images.size() ) : positions;
		this.relativeCellMargin = 0;// relativeCellMargin;
		this.name = name;

		setMinMaxPos();
		configureMipmapAndTileDimensions();
		setTileRealDimensions( tileDimensions[ 0 ] );
		if ( debug )
		{
			System.out.println( "StitchedImage: Min pos: " + Arrays.toString( minPos ) );
			System.out.println( "StitchedImage: Tile real dimensions: " + Arrays.toString( tileRealDimensions ) );
		}

		// Transform the individual images that make up the tiles.
		// And use those images as the basis for the stitched image.
		// This is needed for a {@code RegionDisplay}
		// to know the location of the annotated images.
		// This also is needed because for annotated images
		// the annotations (both the table and the AnnotationType pixels)
		// need to be transformed.
		transform( images, referenceMask );

		// Create the stitched image
		stitch();
	}

	private double[] computeTileMarginOffset( double[] tileRealDimensions, RealInterval imageRealInterval )
	{
		final double[] imageRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
			imageRealDimensions[ d ] = ( imageRealInterval.realMax( d ) - imageRealInterval.realMin( d ) );

		final double[] translationOffset = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			translationOffset[ d ] = 0.5 * ( tileRealDimensions[ d ] - imageRealDimensions[ d ] );

		return translationOffset;
	}

	protected void transform( List< ? extends Image< ? > > images, RealMaskRealInterval referenceMask )
	{
		final double[] offset = computeTileMarginOffset( tileRealDimensions, referenceMask );

		final List< List< ? extends Image< ? > > > nestedImages = new ArrayList<>();
		List< List< String > > nestedTransformedNames = new ArrayList<>();

		for ( Image< ? > image : images )
		{
			final List< Image< ? > > imagesAtGridPosition = new ArrayList<>();
			final List< String > imagesNamesAtGridPosition = new ArrayList<>();

			// Only the metadataImage may have been loaded.
			// Avoid premature loading of the other images
			// by providing the metadata
			// of the metadataImage.
			// Note that for a StitchedImage all images
			// are required to have the same metadata.
			// Right now the only metadata
			// that is needed is the mask.

			// Create a copy of the mask, because
			// it may be transformed within the image.
			final double[] min = referenceMask.minAsDoubleArray();
			final double[] max = referenceMask.maxAsDoubleArray();
			final RealMaskRealInterval mask = GeomMasks.closedBox( min, max );
			image.setMask( mask );

			imagesAtGridPosition.add( image );
			imagesNamesAtGridPosition.add( image.getName() + "_" + name );

			if ( image instanceof StitchedImage )
			{
				// Also transform the image tiles that are contained
				// in the stitched image.
				// Here, we don't need to use metadataImage,
				// because the images of those tiles
				// are already initialised.
				final List< String > tileNames = ( ( StitchedImage< ?, ? > ) image ).getImages().stream().map( i -> i.getName() ).collect( Collectors.toList() );
				final Set< Image< ? > > stitchedImages = DataStore.getImageSet( tileNames );
				for ( Image< ? > containedImage : stitchedImages )
				{
					if ( containedImage instanceof StitchedImage )
						throw new UnsupportedOperationException("Nested stitching of MergedGridTransformation is currently not supported.");

					imagesAtGridPosition.add( containedImage );
					imagesNamesAtGridPosition.add( containedImage.getName() + "_" + name );
				}
			}

			nestedImages.add( imagesAtGridPosition );
			nestedTransformedNames.add( imagesNamesAtGridPosition );
		}

		nestedTransformedNames = null; // <- triggers in place transformations
		// List< ? extends Image< ? > > translatedImages =
		ImageTransformer.gridTransform( nestedImages, nestedTransformedNames, positions, tileRealDimensions, false, offset );

		// return translatedImages;
	}

	public List< ? extends Image< ? > > getImages()
	{
		return images;
	}

	protected void stitch()
	{
		final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[ mipmapScales.length ];
		for ( int level = 0; level < mipmapScales.length; ++level )
		{
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			//final double[] offsets = Arrays.stream( levelToTileMarginVoxelTranslation.get( level ) ).map( d -> d - ( long ) d ).toArray();
			final double[] translations = new double[ 3 ];
			for ( int d = 0; d < 3; d++ )
				translations[ d ] = 0.5 * ( mipmapScales[ level ][ d ] - 1 ); // + offsets[ d ];
			mipmapTransform.set(
					mipmapScales[ level ][ 0 ], 0, 0, translations[ 0 ],
					0, mipmapScales[ level ][ 1 ], 0, translations[ 1 ],
					0, 0, mipmapScales[ level ][ 2 ], translations[ 2 ] );
			mipmapTransform.preConcatenate( sourceTransform );
			mipmapTransforms[ level ] = mipmapTransform;
		}

		final RandomAccessibleSupplier randomAccessibleSupplier = new RandomAccessibleSupplier();

		// non-volatile
		//
		final List< RandomAccessibleInterval< T > > mipMapRAIs = createStitchedRAIs( randomAccessibleSupplier );

		final RandomAccessibleIntervalMipmapSource< T > source = new RandomAccessibleIntervalMipmapSource<>(
				mipMapRAIs,
				type,
				voxelDimensions,
				name,
				mipmapTransforms );
		transformedSource = new TransformedSource<>( source );

		// volatile
		//
		final List< RandomAccessibleInterval< V > > volatileMipMapRAIs = createVolatileStitchedRAIs( randomAccessibleSupplier );

		final RandomAccessibleIntervalMipmapSource< V > volatileSource = new RandomAccessibleIntervalMipmapSource<>(
				volatileMipMapRAIs,
				volatileType,
				voxelDimensions,
				name,
				mipmapTransforms );
		final TransformedSource transformedVolatileSource = new TransformedSource( volatileSource, new TransformedSource( volatileSource, transformedSource ) );

		// combined
		//
		sourcePair = new DefaultSourcePair<>( transformedSource, transformedVolatileSource );
	}

	protected List< RandomAccessibleInterval< V > > createVolatileStitchedRAIs( RandomAccessibleSupplier randomAccessibleSupplier )
	{
		final List< RandomAccessibleInterval< V >> stitchedMipMapRAIs = new ArrayList<>();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final V background = volatileType.createVariable();
			background.setValid( true );
			final FunctionRandomAccessible< V > randomAccessible = new FunctionRandomAccessible( 3, new StitchedLocationToValueSupplier( randomAccessibleSupplier, level, background ).get(), () -> volatileType.createVariable() );
			final IntervalView< V > rai = Views.interval( randomAccessible, getInterval( level ) );
			stitchedMipMapRAIs.add( rai );
		}

		return stitchedMipMapRAIs;
	}

	class StitchedLocationToValueSupplier implements Supplier< BiConsumer< Localizable, V > >
	{
		private final RandomAccessibleSupplier randomAccessibleSupplier;
		private final int level;
		private int[] tileDimension;
		private final V background;

		public StitchedLocationToValueSupplier( RandomAccessibleSupplier randomAccessibleSupplier, int level, V background )
		{
			this.randomAccessibleSupplier = randomAccessibleSupplier;
			this.level = level;
			this.tileDimension = tileDimensions[ level ];
			this.background = background;
		}

		@Override
		// This essentially returns a randomAccess.
		// As this is a stitched image, it needs
		// to stitch the random accesses of the tiles.
		// Thus, it internally needs to hold onto several random accesses.
		public synchronized BiConsumer< Localizable, V > get()
		{
			return new StitchedLocationToValue();
		}

		class StitchedLocationToValue implements BiConsumer< Localizable, V >
		{
			public StitchedLocationToValue()
			{
			}

			@Override
			public void accept( Localizable localizable, V volatileOutput )
			{
				int x = localizable.getIntPosition( 0 );
				int y = localizable.getIntPosition( 1 );
				final int xTileIndex = x / tileDimension[ 0 ];
				final int yTileIndex = y / tileDimension[ 1 ];

				if ( ! randomAccessibleSupplier.exists( level, xTileIndex, yTileIndex ) )
				{
					volatileOutput.set( background.copy() );
					volatileOutput.setValid( true );
					return;
				}

				final Status status = randomAccessibleSupplier.getStatus( level, xTileIndex, yTileIndex );

				if ( status.equals( Status.Closed ) )
				{
					// set opening status already now, because we do not
					// know when the executor service will run the actual opening
					//randomAccessibleSupplier.setStatus( level, xTileIndex, yTileIndex, Status.Opening );

					ThreadHelper.stitchedImageExecutorService.submit(
							new RandomAccessibleOpener( level, xTileIndex, yTileIndex )
					);

					volatileOutput.setValid( false );
				}
				else if ( status.equals( Status.Opening ) )
				{
					volatileOutput.setValid( false );
				}
				else if ( status.equals( Status.Open ) )
				{
					// FIXME: The margin logic could be here!
					//   then we would not need to translate the individual RAIs
					//   this could improve performance and may help with the
					//   jumping between resolution layers!
					//   lower resolutions are closer to 0,0
					//   higher resolutions are further
					//   => jump to bottom right
					x = x - xTileIndex * tileDimension[ 0 ];
					y = y - yTileIndex * tileDimension[ 1 ];
					final int z = localizable.getIntPosition( 2 );
					// Instead of calling getAt(... )
					// I tried caching the random accesses
					// but got some weird bug
					// I removed this to keep it simpler.
					// This logic could be added back, if needed.
					// For this it would be good to know if that actually does
					// yield a performance improvement; something to be to
					// discussed with @tpietzsch
					final V volatileType = randomAccessibleSupplier.getVolatileRandomAccessible( level, xTileIndex, yTileIndex ).getAt( x, y, z );
					volatileOutput.set( volatileType );
				}
			}
		}

		class RandomAccessibleOpener implements Runnable
		{
			private final int level;
			private final int xTileIndex;
			private final int yTileIndex;

			public RandomAccessibleOpener( int level, int xTileIndex, int yTileIndex )
			{
				//System.out.println( name + " l" + level + " x" + xTileIndex + " y" + yTileIndex + " i" + valueSupplierIndex.incrementAndGet() );

				this.level = level;
				this.xTileIndex = xTileIndex;
				this.yTileIndex = yTileIndex;
			}

			@Override
			public void run()
			{
				randomAccessibleSupplier.open( level, xTileIndex, yTileIndex );
			}
		}
	}

	protected List< RandomAccessibleInterval< T > > createStitchedRAIs( RandomAccessibleSupplier randomAccessibleSupplier )
	{
		final List< RandomAccessibleInterval< T >> stitchedRAIs = new ArrayList<>();
		for ( int l = 0; l < numMipmapLevels; l++ )
		{
			final int[] cellDimension = tileDimensions[ l ];
			final int level = l;
			BiConsumer< Localizable, T > biConsumer = ( location, value ) ->
			{
				int x = location.getIntPosition( 0 );
				int y = location.getIntPosition( 1 );
				final int xTileIndex = x / cellDimension[ 0 ];
				final int yTileIndex = y / cellDimension[ 1 ];
				x = x - xTileIndex * cellDimension [ 0 ];
				y = y - yTileIndex * cellDimension [ 1 ];

				if( ! randomAccessibleSupplier.exists( level, xTileIndex, yTileIndex  ) )
				{
					// background
					value.set( type.createVariable() );
				}

				// TODO
				//   This is not very efficient, but right now this mainly
				//   is needed to fetch single pixel values when
				//   a user clicks on an image
				//   For rendering screenshots it will be more
				//   efficient to implement this in the same way as for the
				//   volatile version (see below).
				randomAccessibleSupplier.open( level, xTileIndex, yTileIndex );
				final T t = randomAccessibleSupplier.getRandomAccessible( level, xTileIndex, yTileIndex ).randomAccess().setPositionAndGet( x, y, location.getIntPosition( 2 ) );
				value.set( t );
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

	protected void configureMipmapAndTileDimensions( )
	{
		final double[][] voxelSizes = new double[ numMipmapLevels ][ numDimensions ];
		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final AffineTransform3D affineTransform3D = levelToSourceTransform.get( level );
			for ( int d = 0; d < numDimensions; d++ )
				voxelSizes[ level ][ d ] =
					Math.sqrt(
						affineTransform3D.get( 0, d ) * affineTransform3D.get( 0, d ) +
						affineTransform3D.get( 1, d ) * affineTransform3D.get( 1, d ) +	    						affineTransform3D.get( 2, d ) * affineTransform3D.get( 2, d )
					);
		}

		downSamplingFactors = new double[ numMipmapLevels ][ numDimensions ];
		mipmapScales = new double[ numMipmapLevels ][ numDimensions ];

		// level 0
		for ( int d = 0; d < numDimensions; d++ )
		{
			downSamplingFactors[ 0 ][ d ] = 1.0;
			mipmapScales[ 0 ][ d ] = 1.0 / downSamplingFactors[ 0 ][ d ];
		}

		// level 1 to N
		for ( int level = 1; level < numMipmapLevels; level++ )
		{
			for ( int d = 0; d < numDimensions; d++ )
			{
				downSamplingFactors[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ level - 1 ][ d ];
				mipmapScales[ level ][ d ] = voxelSizes[ level ][ d ] / voxelSizes[ 0 ][ d ];
			}
		}

		// tileDimensions level 0
		tileDimensions = new int[ numMipmapLevels ][ numDimensions ];
		tileDimensions[ 0 ] = MoBIEHelper.asInts( levelToSourceDimensions.get( 0 ) );
		for ( int d = 0; d < 2; d++ )
			tileDimensions[ 0 ][ d ] *= ( 1 + 2.0 * relativeCellMargin );

		// tileDimensions level 1 to N
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				tileDimensions[ level ][ d ] = (int) ( tileDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );

		// marginTranslations
		marginTranslations = new double[ numMipmapLevels ][ numDimensions ];;
		for ( int level = 0; level < numMipmapLevels; level++ )
			marginTranslations[ level ] = marginTranslation( tileDimensions[ level ], levelToSourceDimensions.get( level ) );

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

	@Deprecated // no getting this from the sourcePair
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

	private double[] marginTranslation( int[] tileDimensions, long[] dataDimensions )
	{
		// FIXME: Can we be more precise here?
		//  Could this lead to jumps between resolution levels?
		final double[] translation = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
			translation[ d ] = ( tileDimensions[ d ] - dataDimensions[ d ] ) / 2.0;
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		// FIXME Maybe here is the right place to also transform the contained images?!
		final AffineTransform3D transform3D = new AffineTransform3D();
		transformedSource.getFixedTransform( transform3D );
		transform3D.preConcatenate( affineTransform3D );
		transformedSource.setFixedTransform( transform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		final RealMaskRealInterval mask = SourceHelper.estimateMask( getSourcePair().getSource(), 0 );
		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}

	enum Status // for RandomAccessSupplier
	{
		Closed,
		Opening,
		Open;
	}

	class RandomAccessibleSupplier
	{
		protected Map< String, RandomAccessible< T > > keyToRandomAccessible;
		protected Map< String, RandomAccessible< V > > keyToVolatileRandomAccessible;
		protected Map< String, Status > keyToStatus;
		protected Map< String, Image< T > > keyToImage;

		public RandomAccessibleSupplier( )
		{
			keyToRandomAccessible = new ConcurrentHashMap<>();
			keyToVolatileRandomAccessible = new ConcurrentHashMap<>();
			keyToStatus = new ConcurrentHashMap<>();
			keyToImage = new ConcurrentHashMap<>();

			for ( int gridIndex = 0; gridIndex < positions.size(); gridIndex++ )
			{
				final int[] position = positions.get( gridIndex );
				keyToImage.put( getKey( position[ 0 ], position[ 1 ] ), images.get( gridIndex ) );

				for ( int level = 0; level < numMipmapLevels; level++ )
					keyToStatus.put( getKey( level, position[ 0 ], position[ 1 ] ), Status.Closed );
			}
		}

		public RandomAccessible< T > getRandomAccessible( int level, int xTileIndex, int yTileIndex )
		{
			return keyToRandomAccessible.get( getKey( level, xTileIndex, yTileIndex ) );
		}

		public RandomAccessible< V > getVolatileRandomAccessible( int level, int xTileIndex, int yTileIndex )
		{
			return keyToVolatileRandomAccessible.get( getKey( level, xTileIndex, yTileIndex ) );
		}

		private String getKey( int level, long x, long y )
		{
			return level + "-" + x + "-" + y;
		}

		public Status getStatus( int level, int xTileIndex, int yTileIndex )
		{
			return keyToStatus.get( getKey( level, xTileIndex, yTileIndex ) );
		}

		public void setStatus( int level, int xTileIndex, int yTileIndex, Status status )
		{
			keyToStatus.put( getKey( level, xTileIndex, yTileIndex ), status );
		}

		public boolean exists( int level, int xTileIndex, int yTileIndex )
		{
			return keyToStatus.containsKey( getKey( level, xTileIndex, yTileIndex ) );
		}

		public void open( int level, int xTileIndex, int yTileIndex )
		{
			final String key = getKey( level, xTileIndex, yTileIndex );

			synchronized ( keyToStatus )
			{
				if ( keyToStatus.get( key ).equals( Status.Open ) )
					return;

				keyToStatus.put( key, Status.Opening );
			}

			int t = 0; // TODO

			// open the source.
			//
			final Image< T > image = keyToImage.get( getKey( xTileIndex, yTileIndex ) );
			final RandomAccessibleInterval< T > rai = Views.zeroMin( image.getSourcePair().getSource().getSource( t, level ) );
			final RandomAccessibleInterval< ? extends Volatile< T > > vRai = Views.zeroMin(  image.getSourcePair().getVolatileSource().getSource( t, level ) );

			// extend bounds to be able to
			// accommodate grid margin
			//
			final T outOfBoundsVariable = type.createVariable();
			RandomAccessible< T > randomAccessible = new ExtendedRandomAccessibleInterval( rai, new OutOfBoundsConstantValueFactory<>( outOfBoundsVariable ) );

			RandomAccessible< V > vRandomAccessible = new ExtendedRandomAccessibleInterval( vRai, new OutOfBoundsConstantValueFactory<>( volatileType.createVariable() ) );

			// shift to create a margin
			// FIXME it could be that this is not precise enough and thus creates
			//       some jumps in the resolution layers
			//
			final long[] translation = Arrays.stream( marginTranslations[ level ] ).mapToLong( d -> ( long ) d ).toArray();
			final RandomAccessible< T > translateRa = Views.translate( randomAccessible, translation );
			final RandomAccessible< V > translateVRa = Views.translate( vRandomAccessible, translation );

			// ensure that random access is really ready to go
			// (i.e. all metadata are fetched).
			// this is important to avoid any blocking in BDV.
			//
			translateRa.randomAccess().get();
			translateVRa.randomAccess().get();

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
