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
package org.embl.mobie.lib.image;

import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
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
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.io.Status;
import org.embl.mobie.lib.source.MoBIEVolatileTypeMatcher;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.transform.TransformHelper;
import org.embl.mobie.lib.transform.image.ImageTransformer;

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
	public static final String SEP = ",";
	private final T type;
	private final String name;
	private List< ? extends Image< T > > images;
	private List< int[] > positions;
	private final double relativeTileMargin;
	private int[][] tileDimensions;
	private double[][] downSamplingFactors;
	private double[][] mipmapScales;
	private double[][] marginTranslations;
	private HashMap< Integer, AffineTransform3D > levelToSourceTransform;
	private HashMap< Integer, long[] > levelToSourceDimensions;
	private double[] tileRealDimensions;
	private int numMipmapLevels;
	private DefaultSourcePair< T > sourcePair;
	private V volatileType;
	private long[] minPos;
	private long[] maxPos;
	private int numDimensions;
	private VoxelDimensions voxelDimensions;
	private TransformedSource< T > transformedSource;
	private RealMaskRealInterval referenceMask;
	private AffineTransform3D sourceTransform;
	private int numTimepoints;

	private final boolean debug = false;
	private RealMaskRealInterval mask;
	private Source< T > metadataSource;

	public StitchedImage( List< ? extends Image< T > > images, Image< T > metadataImage, @Nullable List< int[] > gridPositions, String name, double relativeTileMargin )
	{
		this.images = images;

		// Fetch image dimensions, type and mask from {@code metadataImage}.
		// The {@code metadataImage} does not need to be part of the StitchedImage;
		// in fact, this is the whole point, as we can avoid loading
		// any of the images of the StitchedImage at this point.
		// This can make initialisation much faster.
		// This is useful if there are many StitchedImages to be initialised
		// such as the wells of an HTM screen.
		this.metadataSource = metadataImage.getSourcePair().getSource();
		this.type = metadataSource.getType().createVariable();
		this.volatileType = ( V ) MoBIEVolatileTypeMatcher.getVolatileTypeForType( type );
		this.numTimepoints = SourceHelper.getNumTimepoints( metadataSource );
		this.numMipmapLevels = metadataSource.getNumMipmapLevels();
		this.numDimensions = metadataSource.getVoxelDimensions().numDimensions();
		this.voxelDimensions = metadataSource.getVoxelDimensions();
		this.levelToSourceTransform = new HashMap<>();
		this.levelToSourceDimensions = new HashMap<>();

		// Get the reference mask from the metadata image.
		// We only need the spatial extent of the image.
		// The absolute position will be computed based on the tile
		// position and, thus, we remove any spatial offset.
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
				System.out.println( "StitchedImage: Number of tiles: " + gridPositions.size() );

			}

			levelToSourceTransform.put( level, copy );
			levelToSourceDimensions.put( level, dimensions );
		}

		this.sourceTransform = levelToSourceTransform.get( 0 );
		setPositions( images, gridPositions );
		this.relativeTileMargin = relativeTileMargin;
		this.name = name;

		setMinMaxPos();
		configureMipmapAndTileDimensions();
		setTileRealDimensions( tileDimensions[ 0 ] );
		if ( debug )
		{
			System.out.println( "StitchedImage: Min pos: " + Arrays.toString( minPos ) );
			System.out.println( "StitchedImage: Tile real dimensions: " + Arrays.toString( tileRealDimensions ) );
		}

		// Transform the individual images that make up the tiles,
		// and use those images as the basis for the stitched image.
		// This is needed for a {@code RegionDisplay}
		// to know the location of the annotated images.
		// This also is needed because for annotated images
		// the annotations (both the table and the AnnotationType pixels)
		// need to be transformed.
		transform( images, referenceMask );

		// Create the stitched image
		stitch();
	}

	private void setPositions( List< ? extends Image< T > > images, List< int[] > positions )
	{
		if ( positions == null )
		{
			this.positions = TransformHelper.createGridPositions( images.size() );
		}
		else
		{
			// zero-min the positions, because,
			// I don't know why but, when the positions
			// have a non-zero offset the rendered tiles
			// do not match the RegionImage annotation
			// locations
			final int[] min = new int[ 2 ];
			for ( int d = 0; d < 2; d++ )
			{
				int finalD = d;
				min[ d ] = positions.stream().mapToInt( pos -> pos[ finalD ] ).min().getAsInt();
			}

			this.positions = new ArrayList<>();
			for ( int[] position : positions )
			{
				final int[] thisPosition = new int[ 2 ];
				for ( int d = 0; d < 2; d++ )
				{
					thisPosition[ d ] = position[ d ] - min[ d ];
				}
				this.positions.add( thisPosition );
			}
		}
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
				// Here, we don't need to use {@code metadataImage},
				// because the images of those tiles
				// are already initialised.
				final List< String > tileNames = ( ( StitchedImage< ?, ? > ) image ).getImages().stream().map( i -> i.getName() ).collect( Collectors.toList() );
				final Set< Image< ? > > stitchedImages = DataStore.getImageSet( tileNames );
				for ( Image< ? > containedImage : stitchedImages )
				{
					if ( containedImage instanceof StitchedImage )
					{
						throw new UnsupportedOperationException( "Nested stitching of MergedGridTransformation is currently not supported." );
					}

					imagesAtGridPosition.add( containedImage );
					imagesNamesAtGridPosition.add( containedImage.getName() + "_" + name );
				}
			}

			nestedImages.add( imagesAtGridPosition );
			nestedTransformedNames.add( imagesNamesAtGridPosition );
		}

		nestedTransformedNames = null; // <- triggers in place transformations

		if ( debug )
		{
			for ( List< ? extends Image< ? > > nestedImage : nestedImages )
			{
				for ( Image< ? > image : nestedImage )
				{
					System.out.println( "Initial image: " + image.getName() + ": " + Arrays.toString( image.getMask().minAsDoubleArray() ) + " - " + Arrays.toString( image.getMask().maxAsDoubleArray() ));
				}
			}
		}

		ImageTransformer.gridTransform( nestedImages, nestedTransformedNames, positions, tileRealDimensions, false, offset );

		if ( debug )
		{
			for ( List< ? extends Image< ? > > nestedImage : nestedImages )
			{
				for ( Image< ? > image : nestedImage )
				{
					System.out.println( "Transformed image: " + image.getName() + ": " + Arrays.toString( image.getMask().minAsDoubleArray() ) + " - " + Arrays.toString( image.getMask().maxAsDoubleArray() ));
				}
			}
		}

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

			final double[] translations = new double[ 3 ];

			for ( int d = 0; d < 3; d++ )
				translations[ d ] = 0.5 * ( mipmapScales[ level ][ d ] - 1 );

			mipmapTransform.set(
					mipmapScales[ level ][ 0 ], 0, 0, translations[ 0 ],
					0, mipmapScales[ level ][ 1 ], 0, translations[ 1 ],
					0, 0, mipmapScales[ level ][ 2 ], translations[ 2 ] );

			mipmapTransform.preConcatenate( sourceTransform );

			mipmapTransforms[ level ] = mipmapTransform;
		}

		final TileStore tileStore = new TileStore( images, positions );

		// non-volatile
		//
		final Map< Integer, List< RandomAccessibleInterval< T > > > timepointToRAI = stitchTiles( tileStore );

		final StitchedSource< T > source = new StitchedSource<>(
				timepointToRAI,
				type,
				voxelDimensions,
				name,
				mipmapTransforms );
		transformedSource = new TransformedSource<>( source );

		// volatile
		//
		final Map< Integer, List< RandomAccessibleInterval< V > > > timepointToVolatileRAI = stitchVolatileTiles( tileStore );

		final StitchedSource< V > volatileSource = new StitchedSource<>(
				timepointToVolatileRAI,
				volatileType,
				voxelDimensions,
				name,
				mipmapTransforms );

		final TransformedSource transformedVolatileSource = new TransformedSource( volatileSource, new TransformedSource( volatileSource, transformedSource ) );

		// combined
		//
		sourcePair = new DefaultSourcePair<>( transformedSource, transformedVolatileSource );
	}

	class StitchedSource< T extends Type< T > > implements Source< T >
	{
		private final Map< Integer, List< RandomAccessibleInterval< T > > > mipmapSources;
		private final AffineTransform3D[] mipmapTransforms;
		private final VoxelDimensions voxelDimensions;
		private final T type;
		private final String name;
		private final DefaultInterpolators< ? extends NumericType > interpolators;

		public StitchedSource(
				final Map< Integer, List< RandomAccessibleInterval< T > > > stitched,
				final T type,
				final VoxelDimensions voxelDimensions,
				final String name,
				AffineTransform3D[] mipmapTransforms )
		{
			this.type = type;
			this.name = name;
			assert stitched.size() == mipmapTransforms.length : "Number of mipmaps and scale factors do not match.";

			this.mipmapSources = stitched;
			this.mipmapTransforms = mipmapTransforms;
			this.interpolators = new DefaultInterpolators<>();
			this.voxelDimensions = voxelDimensions;
		}

		@Override
		public RandomAccessibleInterval< T > getSource( final int t, final int level )
		{
			return mipmapSources.get( t ).get( level );
		}

		@Override
		public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
		{
			transform.set( mipmapTransforms[ level ] );
		}

		@Override
		public VoxelDimensions getVoxelDimensions()
		{
			return voxelDimensions;
		}

		@Override
		public int getNumMipmapLevels()
		{
			return mipmapTransforms.length;
		}

		@Override
		public boolean isPresent( int t )
		{
			return mipmapSources.containsKey( t );
		}

		@Override
		public boolean doBoundingBoxCulling()
		{
			return true;
		}

		@Override
		public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
		{
			if ( type instanceof NumericType )
			{
				final RandomAccessible ra = Views.extendZero( ( RandomAccessibleInterval ) getSource( t, level ) );
				return ( RealRandomAccessible< T > ) Views.interpolate( ra, interpolators.get( method ) );
			}
			else
			{
				final T outOfBoundsVariable = type.createVariable();
				final RandomAccessible ra = new ExtendedRandomAccessibleInterval<>( getSource( t, level ), new OutOfBoundsConstantValueFactory<>( outOfBoundsVariable ) );
				return Views.interpolate( ra, new NearestNeighborInterpolatorFactory< T >() );
			}
		}

		@Override
		public T getType()
		{
			return type;
		}

		@Override
		public String getName()
		{
			return name;
		}
	}

	protected Map< Integer, List< RandomAccessibleInterval< V > > > stitchVolatileTiles( TileStore tileStore )
	{
		final Map< Integer, List< RandomAccessibleInterval< V > > > stitched = new HashMap<>();

		for ( int t = 0; t < numTimepoints; t++ )
		{
			stitched.put( t, new ArrayList<>() );
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				final V background = volatileType.createVariable();
				background.setValid( true );
				final FunctionRandomAccessible< V > stichedTimepointAtLevel = new FunctionRandomAccessible( 3, new VolatileValueFromTilesFetcherSupplier( tileStore, t, level, background ).get(), () -> volatileType.createVariable() );
				final IntervalView< V > rai = Views.interval( stichedTimepointAtLevel, getInterval( level ) );
				stitched.get( t ).add( rai );
			}
		}

		return stitched;
	}

	class VolatileValueFromTilesFetcherSupplier implements Supplier< BiConsumer< Localizable, V > >
	{
		private final TileStore tileStore;
		private final int t;
		private final int level;
		private int[] tileDimension;
		private final V background;

		public VolatileValueFromTilesFetcherSupplier( TileStore tileStore, int t, int level, V background )
		{
			this.tileStore = tileStore;
			this.t = t;
			this.level = level;
			this.tileDimension = tileDimensions[ level ];
			this.background = background;
		}

		@Override
		public synchronized BiConsumer< Localizable, V > get()
		{
			return new VolatileValueFromTilesFetcher();
		}


		/**
		 * {@code ValueFromTilesFetcher} feeds the {@code FunctionRandomAccessible}
		 * and thereby builds one large (stitched) RandomAccessible from (many)
		 * smaller ones.
		 *
		 * This approach has certain features that solve some issues
		 * that I had with other approaches.
		 *
		 * 1. No additional caching is needed as one directly forwards
		 *    the volatile values from the underlying {@code Image} tiles.
		 * 2. This also means that one does not have to decide some grid (cell)
		 *    size as one would have to do using a {@code CachedCellImg}.
		 * 3. One can position the {@code Image} tiles freely, as one can
		 *    decide within the {@code accept} method from where
		 *    in which {@code Image} one fetches the {@code volatileValue}
		 *    corresponding to the {@code location}.
		 * 4. One can avoid blocking of BDV that could happen when individually
		 *    displaying all the {@code Image} tiles,
		 *    when having to wait during {@code getSource( t, level )};
		 *    an issue in particular when opening from S3.
		 *    Here, this is dealt with by returning invalid volatiles until the
		 *    {@code getSource( t, level )} is done (this happens in the
		 *    {@code TileStore}).
		 *
		 *    TODO: Tobias
		 *       Discuss potential drawbacks of this approach...
		 *       1. probably quite some computations for fetching each pixel?
		 */
		class VolatileValueFromTilesFetcher implements BiConsumer< Localizable, V >
		{
			@Override
			public void accept( Localizable location, V volatileValue )
			{
				int x = location.getIntPosition( 0 );
				int y = location.getIntPosition( 1 );
				final int xTileIndex = x / tileDimension[ 0 ];
				final int yTileIndex = y / tileDimension[ 1 ];

				if ( ! tileStore.contains( t, level, xTileIndex, yTileIndex ) )
				{
					volatileValue.set( background.copy() );
					volatileValue.setValid( true );
					return;
				}

				final Status status = tileStore.getStatus( t, level, xTileIndex, yTileIndex );

				if ( status.equals( Status.Closed ) )
				{
					ThreadHelper.stitchedImageExecutorService.submit( new TileOpener( t, level, xTileIndex, yTileIndex ) );

					volatileValue.setValid( false );
				}
				else if ( status.equals( Status.Opening ) )
				{
					volatileValue.setValid( false );
				}
				else if ( status.equals( Status.Open ) )
				{
					// TODO: The margin logic could be here!
					//   then we would not need to translate the individual RAIs
					//   this could improve performance and may help with the
					//   jumping between resolution layers.
					//   lower resolutions are closer to 0,0
					//   higher resolutions are further
					//   => jump to bottom right
					x = x - xTileIndex * tileDimension[ 0 ];
					y = y - yTileIndex * tileDimension[ 1 ];
					final int z = location.getIntPosition( 2 );

					// TODO (ask @tpietzsch)
					//   Instead of calling getAt(... )
					//   I tried holding on to the random accesses
					//   but got some buggy behaviour.
					//   I removed this to keep it simpler.
					//   This logic could be added back, if needed.
					//   For this it would be good to know if that actually does
					//   yield a performance improvement
					final V volatileType = tileStore.getVolatileRandomAccessible( t, level, xTileIndex, yTileIndex ).getAt( x, y, z );
					volatileValue.set( volatileType );
				}
			}
		}

		class TileOpener implements Runnable
		{
			private final int t;
			private final int level;
			private final int xTileIndex;
			private final int yTileIndex;

			public TileOpener( int t, int level, int xTileIndex, int yTileIndex )
			{
				//System.out.println( name + " t" + t + "  l" + level + " x" + xTileIndex + " y" + yTileIndex + " i" + valueSupplierIndex.incrementAndGet() );

				this.t = t;
				this.level = level;
				this.xTileIndex = xTileIndex;
				this.yTileIndex = yTileIndex;
			}

			@Override
			public void run()
			{
				tileStore.open( t, level, xTileIndex, yTileIndex );
			}
		}
	}

	protected Map< Integer, List< RandomAccessibleInterval< T > > > stitchTiles( TileStore tileStore )
	{
		final Map< Integer, List< RandomAccessibleInterval< T > > > stitched = new HashMap<>();

		for ( int t = 0; t < numTimepoints; t++ )
		{
			stitched.put( t, new ArrayList<>() );

			for ( int l = 0; l < numMipmapLevels; l++ )
			{
				final int[] tileDimension = tileDimensions[ l ];
				final int level = l;
				final int timepoint = t;
				BiConsumer< Localizable, T > biConsumer = ( location, value ) ->
				{
					int x = location.getIntPosition( 0 );
					int y = location.getIntPosition( 1 );
					final int xTileIndex = x / tileDimension[ 0 ];
					final int yTileIndex = y / tileDimension[ 1 ];
					x = x - xTileIndex * tileDimension[ 0 ];
					y = y - yTileIndex * tileDimension[ 1 ];

					System.out.println( "" + x + " " + y );
					if ( ! tileStore.contains( timepoint, level, xTileIndex, yTileIndex ) )
					{
						value.set( type.createVariable() ); // background
						return;
					}

					// this is less efficient as the corresponding volatile
					// implementation, but right now this mainly needed
					// to fetch very few pixel values upon segment selections
					//
					//
					tileStore.open( timepoint, level, xTileIndex, yTileIndex );
					final T type = tileStore.getRandomAccessible( timepoint, level, xTileIndex, yTileIndex ).randomAccess().setPositionAndGet( x, y, location.getIntPosition( 2 ) );
					value.set( type );
				} ;

				final FunctionRandomAccessible< T > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, () -> type.createVariable() );
				final IntervalView< T > rai = Views.interval( randomAccessible, getInterval( level ) );
				stitched.get( t ).add( rai );
			}
		}

		return stitched;
	}

	protected void setTileRealDimensions( int[] tileDimensions )
	{
		tileRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
			tileRealDimensions[ d ] = tileDimensions[ d ] * Affine3DHelpers.extractScale( sourceTransform, d );
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
			tileDimensions[ 0 ][ d ] *= ( 1 + 2.0 * relativeTileMargin );

		// tileDimensions level 1 to N
		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				tileDimensions[ level ][ d ] = (int) ( tileDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );

		// Adapt tile dimensions such that they are divisible
		// by all relative changes of the resolutions between the different levels.
		// This is needed such that the stitched images at the different
		// resolution levels have the same size in voxel space (after
		// multiplying the voxel size at the respective level with the
		// mipmap scaling)
		final double[] downSamplingFactorProducts = new double[ numDimensions ];
		Arrays.fill( downSamplingFactorProducts, 1.0D );

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				downSamplingFactorProducts[ d ] *= downSamplingFactors[ level ][ d ];

		tileDimensions[ 0 ] = MoBIEHelper.asInts( levelToSourceDimensions.get( 0 ) );
		for ( int d = 0; d < 2; d++ )
		{
			tileDimensions[ 0 ][ d ] *= ( 1 + 2.0 * relativeTileMargin );
			tileDimensions[ 0 ][ d ] = (int) ( downSamplingFactorProducts[ d ] * Math.ceil( tileDimensions[ 0 ][ d ] / downSamplingFactorProducts[ d ] ) );
		}

		for ( int level = 1; level < numMipmapLevels; level++ )
			for ( int d = 0; d < numDimensions; d++ )
				tileDimensions[ level ][ d ] = (int) ( tileDimensions[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ] );

		// marginTranslations
		// also here make sure that they are divisible by all resolution
		// levels to avoid any jumps due to rounding issues

		// level 0
		marginTranslations = new double[ numMipmapLevels ][ numDimensions ];
		for ( int d = 0; d < 2; d++ )
		{
			marginTranslations[ 0 ][ d ] = ( int ) ( tileDimensions[ 0 ][ d ] * ( relativeTileMargin / ( 1 + 2 * relativeTileMargin ) ) );
			marginTranslations[ 0 ][ d ] = ( int ) ( downSamplingFactorProducts[ d ] * Math.ceil( marginTranslations[ 0 ][ d ] / downSamplingFactorProducts[ d ] ) );
		}

		// level 1 to N
		for ( int level = 1; level < numMipmapLevels; level++ )
		{
			for ( int d = 0; d < 2; d++ )
			{
				marginTranslations[ level ][ d ] = marginTranslations[ level - 1 ][ d ] / downSamplingFactors[ level ][ d ];
			}
		}

		if ( debug )
		{
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				System.out.println( "Level " + level + "; Margin translation [voxels] = " + Arrays.toString( marginTranslations[ level ] ) );
			}
		}
	}

	protected FinalInterval getInterval( int level )
	{
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];

		// for x and y this is the size of the stitched tiles
		for ( int d = 0; d < 2; d++ )
		{
			min[ d ] = minPos[ d ] * tileDimensions[ level ][ d ];
			max[ d ] = ( maxPos[ d ] + 1 ) * tileDimensions[ level ][ d ];
		}

		// along the z-dimension it simply is the size of the reference source
		min[ 2 ] = metadataSource.getSource( 0, level ).min( 2 );
		max[ 2 ] = metadataSource.getSource( 0, level ).max( 2 );

		if ( debug )
		{
			final double[] size = new double[ 3 ];
			for ( int d = 0; d < 3; d++ )
			{
				size[ d ] = ( max[ d ] - min[ d ] ) * mipmapScales[ level ][ d ];
			}
			System.out.println("Level " + level + "; Size " + Arrays.toString( size ) );
		}

		return new FinalInterval( min, max );
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
		// FIXME Maybe also transform the contained images?!
		final AffineTransform3D transform3D = new AffineTransform3D();
		transformedSource.getFixedTransform( transform3D );
		transform3D.preConcatenate( affineTransform3D );
		transformedSource.setFixedTransform( transform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		// TODO: There seems to be some bug in the logic, because we seem to need
		//       to overwrite what is been set by
		//       setMask( RealMaskRealInterval mask )

		mask = SourceHelper.estimateMask( getSourcePair().getSource(), 0, false );
		final String toString = TransformHelper.maskToString( mask );
		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}

	class TileStore
	{
		// TODO: does it make sense to use something like
		// 	  https://github.com/google/guava/wiki/CachesExplained
		//    here? I am not sure, because (probably) the RandomAccessible and Image
		//    values are already backed by some SoftRef cache?!
		protected Map< String, RandomAccessible< T > > timeLevelTileToRA;
		protected Map< String, RandomAccessible< V > > timeLevelTileToVolatileRA;
		protected Map< String, Image< T > > tileToImage;
		protected Map< String, Status > timeLevelTileToStatus;

		public TileStore( List< ? extends Image< T > > images, List< int[] > positions )
		{
			timeLevelTileToRA = new ConcurrentHashMap<>( );
			timeLevelTileToVolatileRA = new ConcurrentHashMap<>();
			timeLevelTileToStatus = new ConcurrentHashMap<>();
			tileToImage = new ConcurrentHashMap<>();

			for ( int gridIndex = 0; gridIndex < positions.size(); gridIndex++ )
			{
				final int[] position = positions.get( gridIndex );
				tileToImage.put( getTileKey( position[ 0 ], position[ 1 ] ), images.get( gridIndex ) );

				for ( int t = 0; t < numTimepoints; t++ )
					for ( int level = 0; level < numMipmapLevels; level++ )
						timeLevelTileToStatus.put( getKey( t, level, position[ 0 ], position[ 1 ] ), Status.Closed );
			}
		}

		public RandomAccessible< T > getRandomAccessible( int t, int level, int xTileIndex, int yTileIndex )
		{
			return timeLevelTileToRA.get( getKey( t, level, xTileIndex, yTileIndex ) );
		}

		public RandomAccessible< V > getVolatileRandomAccessible( int t, int level, int xTileIndex, int yTileIndex )
		{
			return timeLevelTileToVolatileRA.get( getKey( t, level, xTileIndex, yTileIndex ) );
		}

		private String getTileKey( int xTileIndex, int yTileIndex )
		{
			return xTileIndex + SEP + yTileIndex;
		}

		private String getKey( int t, int level, int xTileIndex, int yTileIndex )
		{
			return t + SEP + level + SEP + xTileIndex + SEP + yTileIndex;
		}

		public Status getStatus( int t, int level, int xTileIndex, int yTileIndex )
		{
			return timeLevelTileToStatus.get( getKey( t, level, xTileIndex, yTileIndex ) );
		}

		public boolean contains( int t, int level, int xTileIndex, int yTileIndex )
		{
			return timeLevelTileToStatus.containsKey( getKey( t, level, xTileIndex, yTileIndex ) );
		}

		public void open( int t, int level, int xTileIndex, int yTileIndex )
		{
			final String key = getKey( t, level, xTileIndex, yTileIndex );

			synchronized ( timeLevelTileToStatus )
			{
				if ( ! timeLevelTileToStatus.get( key ).equals( Status.Closed ) )
					return;

				timeLevelTileToStatus.put( key, Status.Opening );
			}

			// open the image
			//
			final Image< T > image = tileToImage.get( getTileKey( xTileIndex, yTileIndex ) );

			if ( debug )
			{
				System.out.println( "Opening tile image " + key + ": " + image.getName() );
			}

			// fetch the requested volume (t, level)
			//
			final RandomAccessibleInterval< T > rai = Views.zeroMin( image.getSourcePair().getSource().getSource( t, level ) );
			final RandomAccessibleInterval< ? extends Volatile< T > > vRai = Views.zeroMin(  image.getSourcePair().getVolatileSource().getSource( t, level ) );

			// extend bounds to accommodate grid margin
			//
			RandomAccessible< T > randomAccessible = new ExtendedRandomAccessibleInterval( rai, new OutOfBoundsConstantValueFactory<>( type.createVariable() ) );
			RandomAccessible< V > vRandomAccessible = new ExtendedRandomAccessibleInterval( vRai, new OutOfBoundsConstantValueFactory<>( volatileType.createVariable() ) );

			// shift to create grid margin
			//
			final long[] translation = Arrays.stream( marginTranslations[ level ] ).mapToLong( d -> ( long ) d ).toArray();
			final RandomAccessible< T > translateRa = Views.translate( randomAccessible, translation );
			final RandomAccessible< V > translateVRa = Views.translate( vRandomAccessible, translation );

			// ensure that random access is ready to go
			// (i.e. all metadata are fetched)
			// to avoid any blocking in BDV
			//
			try
			{
				//translateRa.randomAccess().get(); (not needed for visualisation)
				translateVRa.randomAccess().get();
			}
			catch ( Exception e )
			{
				// TODO ask Tobias...
				// otherwise, such Exceptions are not visible.
				e.printStackTrace();
			}

			timeLevelTileToRA.put( key, translateRa );
			timeLevelTileToVolatileRA.put( key, translateVRa );
			timeLevelTileToStatus.put( key, Status.Open );

			if ( debug )
			{
				System.out.println( "Opened tile image " + key + ": " + image.getName() );
			}
		}
	}
}
