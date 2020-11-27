/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package de.embl.cba.mobie.n5;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import com.amazonaws.SdkClientException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.*;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.*;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.N5CellLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import static bdv.img.n5.BdvN5Format.*;

public class N5OMEZarrImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
	protected final N5Reader n5;
	protected AbstractSequenceDescription< ?, ?, ? > seq;
	protected ViewRegistrations viewRegistrations;
	private static boolean debug = false;

	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map< Integer, SetupImgLoader > setupImgLoaders = new HashMap<>();

	private volatile boolean isOpen = false;
	private FetcherThreads fetchers;
	private VolatileGlobalCellCache cache;
	private Map< Integer, String > setupPathnames = new HashMap<>(  );

	public N5OMEZarrImageLoader( N5Reader n5Reader, AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this.n5 = n5Reader;
		this.seq = sequenceDescription;
	}

	public N5OMEZarrImageLoader( N5Reader n5Reader ) throws IOException
	{
		this.n5 = n5Reader;
		fetchSequenceDescription();
		fetchViewRegistrations();
	}

	public AbstractSequenceDescription< ?, ?, ? > getSequenceDescription()
	{
		//open();
		seq.setImgLoader( Cast.unchecked( this ) );
		return seq;
	}

	public ViewRegistrations getViewRegistrations()
	{
		return viewRegistrations;
	}

	private void open()
	{
		if ( !isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;

				try
				{
					int maxNumLevels = 0;
					final List< ? extends BasicViewSetup > setups = seq.getViewSetupsOrdered();
					for ( final BasicViewSetup setup : setups )
					{
						final int setupId = setup.getId();
						final SetupImgLoader setupImgLoader = createSetupImgLoader( setupId );
						setupImgLoaders.put( setupId, setupImgLoader );
						maxNumLevels = Math.max( maxNumLevels, setupImgLoader.numMipmapLevels() );
					}

					final int numFetcherThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() );
					final BlockingFetchQueues< Callable< ? > > queue = new BlockingFetchQueues<>( maxNumLevels, numFetcherThreads );
					fetchers = new FetcherThreads( queue, numFetcherThreads );
					cache = new VolatileGlobalCellCache( queue );
				}
				catch ( IOException e )
				{
					throw new RuntimeException( e );
				}

				isOpen = true;
			}
		}
	}

	class Multiscale
	{
		String name;
		Transform transform;
		Dataset[] datasets;
	}

	class Dataset
	{
		String path;
	}

	class Transform
	{
		String[] axes;
		double[] scale;
		double[] translate;
		String[] units;
	}

	private void fetchViewRegistrations()
	{
		try
		{
			ArrayList< ViewRegistration > viewRegistrationList = new ArrayList<>();
			int setupId = 0;
			viewRegistrationList.add( getViewRegistration( setupId, false, null ) );
			addLabelImagesViewRegistration( viewRegistrationList, ++setupId );
			viewRegistrations = new ViewRegistrations( viewRegistrationList );

		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private void addLabelImagesViewRegistration( ArrayList< ViewRegistration > viewRegistrationList, int setupId ) throws IOException
	{
		List< String > labels = n5.getAttribute( getLabelsPathName( setupId ), "labels", List.class );
		if ( labels != null)
		{
			String labelImage = labels.get( 0 );
			viewRegistrationList.add( getViewRegistration( setupId, true, labelImage ) );
		}
	}

	@NotNull
	private ViewRegistration getViewRegistration( int setupId, boolean isLabelImage, String labelImage ) throws IOException
	{
		String pathName = getPathName( setupId );

		if ( isLabelImage )
			pathName = getLabelImagePathName( setupId, labelImage );

		Multiscale[] multiscales = n5.getAttribute( pathName, "multiscales", Multiscale[].class );
		AffineTransform3D transform = new AffineTransform3D();
		double[] scale = multiscales[ 0 ].transform.scale;
		transform.scale( scale[ 0 ], scale[ 1 ], scale[ 2  ]);
		ViewRegistration viewRegistration = new ViewRegistration( 0, setupId, transform );

		return viewRegistration;
	}

	// TODO: fetch sequenceDescription and viewSetup in one function
	private void fetchSequenceDescription()
	{
		try
		{
			ArrayList< TimePoint > timePoints = new ArrayList<>();
			timePoints.add( new TimePoint( 0 ) );

			ArrayList< ViewSetup > viewSetups = new ArrayList<>();
			int setupId = 0;
			ViewSetup viewSetup = getViewSetup( setupId, false, null );
			viewSetups.add( viewSetup );
			addLabelImagesViewSetup( viewSetups, ++setupId );

			seq = new SequenceDescription( new TimePoints( timePoints ), viewSetups );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}


	@NotNull
	private ViewSetup getViewSetup( int setupId, boolean isLabelImage, String labelImage ) throws IOException
	{
		String pathName = isLabelImage ? getLabelImagePathName( setupId, 0 ,labelImage ) : getPathName( setupId, 0 );
		final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );  // only the levels have the data type
		FinalDimensions dimensions = new FinalDimensions( attributes.getDimensions() );

		pathName = isLabelImage ? getLabelImagePathName( setupId, labelImage ) : getPathName( setupId );
		setupPathnames.put( setupId, pathName );
		Multiscale[] multiscales = n5.getAttribute( pathName, "multiscales", Multiscale[].class );
		VoxelDimensions voxelDimensions = new FinalVoxelDimensions( multiscales[ 0 ].transform.units[ 0 ], multiscales[ 0 ].transform.scale );
		Tile tile = new Tile( 0 );
		Channel channel = new Channel( 0 );
		Angle angle = new Angle( 0 );
		Illumination illumination = new Illumination( 0 );
		ViewSetup viewSetup = new ViewSetup( setupId, multiscales[ 0 ].name, dimensions, voxelDimensions, tile, channel, angle, illumination );

		return viewSetup;
	}

	private void addLabelImagesViewSetup( ArrayList< ViewSetup > viewSetups, int setupId ) throws IOException
	{
		List< String > labels = n5.getAttribute( getLabelsPathName( setupId ), "labels", List.class );
		if ( labels != null)
		{
			String labelImage = labels.get( 0 );
			viewSetups.add( getViewSetup( setupId, true, labelImage ) );
		}
	}

	/**
	 * Clear the cache. Images that were obtained from
	 * this loader before {@link #close()} will stop working. Requesting images
	 * after {@link #close()} will cause the n5 to be reopened (with a
	 * new cache).
	 */
	public void close()
	{
		if ( isOpen )
		{
			synchronized ( this )
			{
				if ( !isOpen )
					return;
				fetchers.shutdown();
				cache.clearCache();
				isOpen = false;
			}
		}
	}

	// TODO
	private String getPathName( int setupId )
	{
		return "";
	}

	private String getLabelsPathName( int setupId )
	{
		return "labels";
	}

	private String getLabelImagePathName( int setupId, String image )
	{
		return "labels/" + image;
	}

	private String getLabelImagePathName( int setupId, int level, String image )
	{
		return getLabelImagePathName( setupId, image ) + String.format( "/s%d", level );
	}

	// TODO
	private String getPathName( int setupId, int level )
	{
		return String.format( "s%d", level );
	}

	private String getLevelName( int level )
	{
		return String.format( "s%d", level );
	}

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		open();
		return setupImgLoaders.get( setupId );
	}

	private < T extends NativeType< T >, V extends Volatile< T > & NativeType< V > > SetupImgLoader< T, V > createSetupImgLoader( final int setupId ) throws IOException
	{
		final String pathName = setupPathnames.get( setupId ) + "/s0"; // only the levels have the data type

		final DataType dataType = n5.getAttribute( pathName, DATA_TYPE_KEY, DataType.class );

		switch ( dataType )
		{
		case UINT8:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedByteType(), new VolatileUnsignedByteType() ) );
		case UINT16:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedShortType(), new VolatileUnsignedShortType() ) );
		case UINT32:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedIntType(), new VolatileUnsignedIntType() ) );
		case UINT64:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedLongType(), new VolatileUnsignedLongType() ) );
		case INT8:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new ByteType(), new VolatileByteType() ) );
		case INT16:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new ShortType(), new VolatileShortType() ) );
		case INT32:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new IntType(), new VolatileIntType() ) );
		case INT64:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new LongType(), new VolatileLongType() ) );
		case FLOAT32:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new FloatType(), new VolatileFloatType() ) );
		case FLOAT64:
			return Cast.unchecked( new SetupImgLoader<>( setupId, new DoubleType(), new VolatileDoubleType() ) );
		}
		return null;
	}

	@Override
	public CacheControl getCacheControl()
	{
		open();
		return cache;
	}

	public class SetupImgLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > >
			extends AbstractViewerSetupImgLoader< T, V >
			implements MultiResolutionSetupImgLoader< T >
	{
		private final int setupId;

		private final double[][] mipmapResolutions;

		private final AffineTransform3D[] mipmapTransforms;

		public SetupImgLoader( final int setupId, final T type, final V volatileType ) throws IOException
		{
			super( type, volatileType );
			this.setupId = setupId;
			mipmapResolutions = fetchMipmapResolutions();
			mipmapTransforms = new AffineTransform3D[ mipmapResolutions.length ];
			for ( int level = 0; level < mipmapResolutions.length; level++ )
				mipmapTransforms[ level ] = MipmapTransforms.getMipmapTransformDefault( mipmapResolutions[ level ] );
		}


		/**
		 * TODO: Create classes for multiscales and multiscale such that the Json parser does below stuff.
		 *
		 * @return
		 * @throws IOException
		 */
		private double[][] fetchMipmapResolutions() throws IOException
		{
			final String pathName = setupPathnames.get( setupId );
			List< Map< String, Object > > multiscales = n5.getAttribute( pathName, "multiscales", List.class );
			Map< String, Object > multiscale = multiscales.get(0);
			if ( ! multiscale.get("version").equals( "0.1" ) )
				throw new RuntimeException( "Version " + multiscale.get("version") + " of ome.zarr is not supported." );
			List< List< Double > > scales = ( List< List< Double > > ) multiscale.get( "scales" );
			double[][] mipmapResolutions = new double[ scales.size() ][];
			for ( int r = 0; r < mipmapResolutions.length; r++ )
			{
				mipmapResolutions[ r ] = new double[ scales.get( r ).size() ];
				for ( int d = 0; d < mipmapResolutions[ r ].length; d++ )
				{
					mipmapResolutions[ r ][ d ] = scales.get( r ).get( d );
				}
			}
			return mipmapResolutions;
		}


		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BUDGETED, volatileType );
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
		}

		@Override
		public Dimensions getImageSize( final int timepointId, final int level )
		{
			try
			{
				final String pathName = setupPathnames.get( setupId ) + "/" + getLevelName( level );
				final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
				return new FinalDimensions( attributes.getDimensions() );
			}
			catch( Exception e )
			{
				return null;
			}
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels()
		{
			return mipmapResolutions.length;
		}

		@Override
		public VoxelDimensions getVoxelSize( final int timepointId )
		{
			return null;
		}

		/**
		 * Create a {@link CellImg} backed by the cache.
		 */
		private < T extends NativeType< T > > RandomAccessibleInterval< T > prepareCachedImage( final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type )
		{
			try
			{
				// https://github.com/glencoesoftware/bioformats2raw/blob/master/src/test/java/com/glencoesoftware/bioformats2raw/test/ZarrTest.java#L554
				final String pathName = setupPathnames.get( setupId ) + "/" + getLevelName( level );
				final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
				// ome.zarr is 5D but BDV expects 3D
				final long[] dimensions = Arrays.stream( attributes.getDimensions() ).limit( 3 ).toArray();
				final int[] cellDimensions = Arrays.stream( attributes.getBlockSize() ).limit( 3 ).toArray();
				final CellGrid grid = new CellGrid( dimensions, cellDimensions );

				final int priority = numMipmapLevels() - 1 - level;
				final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );

				final SimpleCacheArrayLoader< ? > loader = createCacheArrayLoader( n5, pathName, timepointId, grid );
				return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
			}
			catch ( IOException e )
			{
				System.err.println( String.format(
						"image data for timepoint %d setup %d level %d could not be found.",
						timepointId, setupId, level ) );
				return Views.interval(
						new ConstantRandomAccessible<>( type.createVariable(), 3 ),
						new FinalInterval( 1, 1, 1 ) );
			}
		}
	}

	private static class ArrayCreator< A, T extends NativeType< T > >
	{
		private final CellGrid cellGrid;
		private final DataType dataType;
		private final BiConsumer< ArrayImg<T,?>, DataBlock<?>> copyFromBlock;

		public ArrayCreator( CellGrid cellGrid, DataType dataType )
		{
			this.cellGrid = cellGrid;
			this.dataType = dataType;
			this.copyFromBlock = N5CellLoader.createCopy( dataType );
		}

		public A createArray( DataBlock< ? > dataBlock, long[] gridPosition )
		{
			long[] cellDims = getCellDims( gridPosition );
			int n = (int) ( cellDims[ 0 ] * cellDims[ 1 ] * cellDims[ 2 ] );

			switch ( dataType )
			{
				case UINT8:
				case INT8:
					byte[] bytes = new byte[ n ];
					copyFromBlock.accept( Cast.unchecked( ArrayImgs.bytes( bytes, cellDims ) ), dataBlock );
					return ( A ) new VolatileByteArray( bytes , true );
				case UINT16:
				case INT16:
					short[] shorts = new short[ n ];
					copyFromBlock.accept( Cast.unchecked( ArrayImgs.shorts( shorts, cellDims ) ), dataBlock );
					return ( A ) new VolatileShortArray( shorts , true );
				case UINT32:
				case INT32:
					return null;
					//return createArray.apply( new IntArrayDataBlock( blockSize, gridPosition, new int[ n ] ) );
				case UINT64:
				case INT64:
					long[] longs = new long[ n ];
					copyFromBlock.accept( Cast.unchecked( ArrayImgs.longs( longs, cellDims ) ), dataBlock );
					return ( A ) new VolatileLongArray( longs , true );
				case FLOAT32:
					return null;
					//return createArray.apply( new FloatArrayDataBlock( blockSize, gridPosition, new float[ n ] ) );
				case FLOAT64:
					return null;
					//return createArray.apply( new DoubleArrayDataBlock( blockSize, gridPosition, new double[ n ] ) );
				default:
					throw new IllegalArgumentException();
			}
		}

		public A createEmptyArray( long[] gridPosition )
		{
			long[] cellDims = getCellDims( gridPosition );
			int n = (int) ( cellDims[ 0 ]* cellDims[ 1 ] * cellDims[ 2 ] );
			switch ( dataType )
			{
				case UINT8:
				case INT8:
					return Cast.unchecked( new VolatileByteArray( new byte[ n ], true ) );
				case UINT16:
				case INT16:
					return Cast.unchecked( new VolatileShortArray( new short[ n ], true ) );
				case UINT32:
				case INT32:
					return null;
					//return createArray.apply( new IntArrayDataBlock( blockSize, gridPosition, new int[ n ] ) );
				case UINT64:
				case INT64:
					return Cast.unchecked( new VolatileLongArray( new long[ n ], true ) );
					//return createArray.apply( new LongArrayDataBlock( blockSize, gridPosition, new long[ n ] ) );
				case FLOAT32:
					return null;
					//return createArray.apply( new FloatArrayDataBlock( blockSize, gridPosition, new float[ n ] ) );
				case FLOAT64:
					return null;
					//return createArray.apply( new DoubleArrayDataBlock( blockSize, gridPosition, new double[ n ] ) );
				default:
					throw new IllegalArgumentException();
			}
		}

		private long[] getCellDims( long[] gridPosition )
		{
			long[] cellMin = new long[ 5 ];
			int[] cellDims = new int[ 5 ];
			cellGrid.getCellDimensions( gridPosition, cellMin, cellDims );
			cellDims[ 3 ] = 1; // channel
			cellDims[ 4 ] = 1; // timepoint
			return Arrays.stream( cellDims ).mapToLong( i -> i ).toArray(); // casting to long for creating ArrayImgs.*
		}
	}

	private static class N5OMEZarrCacheArrayLoader< A > implements SimpleCacheArrayLoader< A >
	{
		private final N5Reader n5;
		private final String pathName;
		private final int timepoint;
		private final DatasetAttributes attributes;
		private final ArrayCreator< A, ? > arrayCreator;

		N5OMEZarrCacheArrayLoader( final N5Reader n5, final String pathName, final int timepoint, final DatasetAttributes attributes, CellGrid grid )
		{
			this.n5 = n5;
			this.pathName = pathName; // includes the level
			this.timepoint = timepoint;
			this.attributes = attributes;
			this.arrayCreator = new ArrayCreator<>( grid, attributes.getDataType() );
		}

		@Override
		public A loadArray( final long[] gridPosition ) throws IOException
		{
			DataBlock< ? > block = null;

			long[] gridPosition5D = new long[ 5 ];
			for ( int d = 0; d < 3; d++ ) gridPosition5D[ d ] = gridPosition[ d ];
			gridPosition5D[ 4 ] = timepoint;
			// TODO: add setupId <=> channel

			try {
				block = n5.readBlock( pathName, attributes, gridPosition5D );
			}
			catch ( SdkClientException e )
			{
				System.err.println( e ); // this happens sometimes, not sure yet why...
			}

			if ( debug )
			{
				if ( block != null )
					System.out.println( pathName + " " + Arrays.toString( gridPosition ) + " " + block.getNumElements() );
				else
					System.out.println( pathName + " " + Arrays.toString( gridPosition ) + " NaN" );
			}

			if ( block == null )
			{
				return arrayCreator.createEmptyArray( gridPosition );
			}
			else
			{
				return arrayCreator.createArray( block, gridPosition );
			}
		}
	}

	public static SimpleCacheArrayLoader< ? > createCacheArrayLoader( final N5Reader n5, final String pathName, int timepointId, CellGrid grid ) throws IOException
	{
		final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
		return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes, grid);
	}
}
