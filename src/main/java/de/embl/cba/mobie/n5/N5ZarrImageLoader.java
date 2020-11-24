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
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static bdv.img.n5.BdvN5Format.*;

public class N5ZarrImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
	protected final N5Reader n5;

	// TODO: it would be good if this would not be needed
	//       find available setups from the n5
	protected final AbstractSequenceDescription< ?, ?, ? > seq;

	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map< Integer, SetupImgLoader > setupImgLoaders = new HashMap<>();

	private volatile boolean isOpen = false;
	private FetcherThreads fetchers;
	private VolatileGlobalCellCache cache;

	public N5ZarrImageLoader( N5Reader n5Reader, AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this.n5 = n5Reader;
		this.seq = sequenceDescription;
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

	// TODO
	private String getPathName( int setupId, int level )
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
		final String pathName = getPathName( setupId, 0 ); // only the levels have the data type

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
			final String pathName = getPathName( setupId );
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
				final String pathName = getPathName( setupId, level );
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
				final String pathName = getPathName( setupId, level );
				final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
				// ome.zarr is 5D but BDV expects 3D
				final long[] dimensions = Arrays.stream( attributes.getDimensions() ).limit( 3 ).toArray();
				final int[] cellDimensions = Arrays.stream( attributes.getBlockSize() ).limit( 3 ).toArray();
				final CellGrid grid = new CellGrid( dimensions, cellDimensions );

				final int priority = numMipmapLevels() - 1 - level;
				final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );

				final SimpleCacheArrayLoader< ? > loader = createCacheArrayLoader( n5, pathName, timepointId );
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

	private static class N5OMEZarrCacheArrayLoader< A > implements SimpleCacheArrayLoader< A >
	{
		private final N5Reader n5;
		private final String pathName;
		private final int timepoint;
		private final DatasetAttributes attributes;
		private final Function< DataBlock< ? >, A > createArray;

		N5OMEZarrCacheArrayLoader( final N5Reader n5, final String pathName, final int timepoint, final DatasetAttributes attributes, final Function< DataBlock< ? >, A > createArray )
		{
			this.n5 = n5;
			this.pathName = pathName; // includes the level
			this.timepoint = timepoint;
			this.attributes = attributes;
			this.createArray = createArray;
		}

		@Override
		public A loadArray( final long[] gridPosition ) throws IOException
		{
			DataBlock< ? > block = null;

			// Map 3D grid position to 5D ome.zarr array
			// TODO: add setupId (channel) and timepoint, which are currently hard coded to 0
			long[] gridPosition5D = new long[ 5 ];
			for ( int d = 0; d < 3; d++ ) gridPosition5D[ d ] = gridPosition[ d ];
			gridPosition5D[ 4 ] = timepoint;

			try {
				block = n5.readBlock( pathName, attributes, gridPosition5D );
			}
			catch ( SdkClientException e )
			{
				System.err.println( e ); // this happens sometimes, not sure yet why...
			}

			if ( block == null )
			{
				final int[] blockSize = attributes.getBlockSize();
				final int n = blockSize[ 0 ] * blockSize[ 1 ] * blockSize[ 2 ];
				switch ( attributes.getDataType() )
				{
					case UINT8:
					case INT8:
						return createArray.apply( new ByteArrayDataBlock( blockSize, gridPosition, new byte[ n ] ) );
					case UINT16:
					case INT16:
						return createArray.apply( new ShortArrayDataBlock( blockSize, gridPosition, new short[ n ] ) );
					case UINT32:
					case INT32:
						return createArray.apply( new IntArrayDataBlock( blockSize, gridPosition, new int[ n ] ) );
					case UINT64:
					case INT64:
						return createArray.apply( new LongArrayDataBlock( blockSize, gridPosition, new long[ n ] ) );
					case FLOAT32:
						return createArray.apply( new FloatArrayDataBlock( blockSize, gridPosition, new float[ n ] ) );
					case FLOAT64:
						return createArray.apply( new DoubleArrayDataBlock( blockSize, gridPosition, new double[ n ] ) );
					default:
						throw new IllegalArgumentException();
				}
			}
			else
			{
				return createArray.apply( block );
			}
		}
	}

	public static SimpleCacheArrayLoader< ? > createCacheArrayLoader( final N5Reader n5, final String pathName, int timepointId ) throws IOException
	{
		final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
		switch ( attributes.getDataType() )
		{
		case UINT8:
		case INT8:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileByteArray( Cast.unchecked( dataBlock.getData() ), true ) );
		case UINT16:
		case INT16:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileShortArray( Cast.unchecked( dataBlock.getData() ), true ) );
		case UINT32:
		case INT32:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileIntArray( Cast.unchecked( dataBlock.getData() ), true ) );
		case UINT64:
		case INT64:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileLongArray( Cast.unchecked( dataBlock.getData() ), true ) );
		case FLOAT32:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileFloatArray( Cast.unchecked( dataBlock.getData() ), true ) );
		case FLOAT64:
			return new N5OMEZarrCacheArrayLoader<>( n5, pathName, timepointId, attributes,
					dataBlock -> new VolatileDoubleArray( Cast.unchecked( dataBlock.getData() ), true ) );
		default:
			throw new IllegalArgumentException();
		}
	}
}
