/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie;

import bdv.cache.SharedQueue;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.measure.Calibration;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.toml.TPosition;
import org.embl.mobie.io.toml.ZPosition;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.DataSource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public abstract class DataStore
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	// Images of the current view
	private static Map< String, Image< ? > > images = new ConcurrentHashMap<>();

	// Currently, only used to pre-load tables for region annotations
	private static Map< String, DataSource > rawData = new ConcurrentHashMap<>();

	public static BiMap< SourceAndConverter< ? >, Image< ? > > sourceToImage()
	{
		return sourceToImage;
	}

	private static BiMap<SourceAndConverter<?>, Image<?>> sourceToImage = HashBiMap.create();

	// TODO: replace by some soft ref cache? How to free the memory?
	private static Map< Object, CompletableFuture< AbstractSpimData< ? > > > spimDataCache = new ConcurrentHashMap<>();

	public static void putSpimData( String path, AbstractSpimData< ? > spimData )
	{
		spimDataCache.put( path, CompletableFuture.completedFuture( spimData ) );
	}

	public static AbstractSpimData< ? > fetchSpimData( String path, ImageDataFormat imageDataFormat, SharedQueue sharedQueue )
	{
		try
		{
			return spimDataCache
					.computeIfAbsent( path,
							p -> CompletableFuture.supplyAsync( ()
							-> openSpimData( (String) p, imageDataFormat, sharedQueue ) ) )
					.get();
		}
		catch ( InterruptedException e )
		{
			throw new RuntimeException( e );
		}
		catch ( ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static AbstractSpimData< ? > fetchSpimData( Site site, SharedQueue sharedQueue )
	{
		try
		{
			return spimDataCache
					.computeIfAbsent(site,
							s -> CompletableFuture.supplyAsync(()
									-> openSpimData( (Site) s, sharedQueue )))
					.get();
		}
		catch ( InterruptedException e )
		{
			throw new RuntimeException( e );
		}
		catch ( ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	private static AbstractSpimData< ? > openSpimData( String path, ImageDataFormat imageDataFormat, SharedQueue sharedQueue )
	{
		try
		{
			AbstractSpimData< ? > spimData = new SpimDataOpener().open( path, imageDataFormat, sharedQueue );
			List< ViewSetup > viewSetupsOrdered = ( List< ViewSetup > ) spimData.getSequenceDescription().getViewSetupsOrdered();

//			System.out.println( "Opening " + path + ", " +imageDataFormat  );
//			for ( ViewSetup viewSetup : viewSetupsOrdered )
//			{
//				System.out.println( "Channel: " + viewSetup.getChannel().getName() );
//			}

			return spimData;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void clearSpimData( )
	{
		spimDataCache = new ConcurrentHashMap<>();
	}

	public static void putRawData( DataSource dataSource )
	{
		rawData.put( dataSource.getName(), dataSource );
	}

	public static DataSource getRawData( String name )
	{
		if ( ! rawData.containsKey( name ) )
			throw new RuntimeException( "The data " + name + " has not been loaded.");

		return rawData.get( name );
	}

	public static boolean containsRawData( String name )
	{
		return rawData.keySet().contains( name );
	}

	public static Image< ? > getImage( String name )
	{
		if ( ! images.containsKey( name ) )
			throw new RuntimeException( name + " is not part of the current image data.");

		return images.get( name );
	}

	// Unsorted Set.
	public static Set< Image< ? > > getImageSet( Collection< String > names )
	{
		final Set< Image< ? > > images = new HashSet<>();
		for ( String name : names )
			images.add( getImage( name ) );
		return images;
	}

	// Sorted List, corresponding to the given names.
	public static List< ? extends Image< ? > > getImageList( List< String > names )
	{
		final ArrayList< Image< ? > > images = new ArrayList<>();
		for ( String name : names )
			images.add( getImage( name ) );
		return images;
	}

	public static void putImage( Image< ? > image )
	{
		if ( image.getName() == null )
			throw new UnsupportedOperationException( "The image " + image.getClass().getSimpleName() + " does not have a name." );

		images.put( image.getName(), image );
	}

	public static void putImages( Collection< ? extends Image< ? > > images )
	{
		for ( Image< ? > image : images )
			DataStore.images.put( image.getName(), image );
	}

	public static void clearImages()
	{
		// FIXME Caching: https://github.com/mobie/mobie-viewer-fiji/issues/813
		images.clear();
	}

	private static AbstractSpimData< ? > openSpimData( Site site, SharedQueue sharedQueue )
	{
		VirtualStack virtualStack = null;

		final Map< TPosition, Map< ZPosition, String > > paths = site.getPaths();

		final ArrayList< TPosition > tPositions = new ArrayList<>( paths.keySet() );
		Collections.sort( tPositions );
		int nT = tPositions.size();
		int nZ = 1;
		for ( TPosition t : tPositions )
		{
			final Set< ZPosition > zPositions = paths.get( t ).keySet();
			nZ = zPositions.size();
			for ( ZPosition z : zPositions )
			{
				if ( virtualStack == null )
				{
					final int[] dimensions = site.getDimensions();
					virtualStack = new VirtualStack( dimensions[ 0 ], dimensions[ 1 ], null, "" );
				}

				virtualStack.addSlice( paths.get( t ).get( z ) );
			}
		}

		final ImagePlus imagePlus = new ImagePlus( site.getId(), virtualStack );

		final Calibration calibration = new Calibration();
		final VoxelDimensions voxelDimensions = site.getVoxelDimensions();
		calibration.setUnit( voxelDimensions.unit() );
		calibration.pixelWidth = voxelDimensions.dimension( 0 );
		calibration.pixelHeight = voxelDimensions.dimension( 1 );
		calibration.pixelDepth = voxelDimensions.dimension( 2 );
		imagePlus.setCalibration( calibration );

		// TODO: is could be zSlices!
		imagePlus.setDimensions( 1, nZ, nT );

		final AbstractSpimData< ? > spimData = ImagePlusToSpimData.getSpimData( imagePlus );
		SpimDataOpener.setSharedQueue( sharedQueue, spimData );

		return spimData;
	}
}
