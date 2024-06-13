/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.ImagePlus;
import ij.measure.Calibration;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.imagedata.ImagePlusImageData;
import org.embl.mobie.io.toml.TPosition;
import org.embl.mobie.io.toml.ZPosition;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.hcs.VirtualStackWithFlexibleLoader;
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

	private static BiMap< SourceAndConverter< ? >, Image< ? > > sourceToImage = HashBiMap.create();

	// TODO: replace by some soft ref cache? How to free the memory?
	private static Map< Object, CompletableFuture< ImageData< ? > > > imageDataCache = new ConcurrentHashMap<>();

	public static BiMap< SourceAndConverter< ? >, Image< ? > > sourceToImage()
	{
		return sourceToImage;
	}

	public static ImageData< ? > fetchImageData(
			String path,
			ImageDataFormat imageDataFormat,
			SharedQueue sharedQueue )
	{
		try
		{
			return imageDataCache
					.computeIfAbsent( path,
							p -> CompletableFuture.supplyAsync( ()
							-> openImageData( (String) p, imageDataFormat, sharedQueue ) ) )
					.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			throw new RuntimeException( e );
		}
    }

	public static ImageData< ? > fetchImageData( Site site, SharedQueue sharedQueue )
	{
		try
		{
			return imageDataCache
					.computeIfAbsent(site,
							s -> CompletableFuture.supplyAsync(()
									-> addImageData( (Site) s, sharedQueue )))
					.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			throw new RuntimeException( e );
		}
    }

	private static ImageData< ? > openImageData( String path, ImageDataFormat imageDataFormat, SharedQueue sharedQueue )
	{
		try
		{
			return ImageDataOpener.open( path, imageDataFormat, sharedQueue );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void clearSpimDataCache( )
	{
		imageDataCache = new ConcurrentHashMap<>();
	}

	public static void addRawData( DataSource dataSource )
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


	public static boolean containsImage( String name )
	{
		return images.containsKey( name );
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

	public static void addImage( Image< ? > image )
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
		// TODO: Caching: https://github.com/mobie/mobie-viewer-fiji/issues/813
		images.clear();
	}

	private static ImageData< ? > addImageData( Site site, SharedQueue sharedQueue )
	{
		VirtualStackWithFlexibleLoader virtualStack = null;

		final Map< TPosition, Map< ZPosition, String > > paths = site.getPaths();

		// TODO: This is a mess....
//		if ( paths.size() == 1 && paths.get( paths.keySet().iterator().next() ).size() == 1 )
//		{
//			Map< ZPosition, String > zPositionStringMap = paths.get( paths.keySet().iterator().next() );
//			String path = zPositionStringMap.get( zPositionStringMap.keySet().iterator().next() );
//			AbstractSpimData< ? > spimData = openSpimData( path, site.getImageDataFormat(), sharedQueue );
//			return spimData;
//		}
//		else
//		{
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
					ImageDataFormat imageDataFormat = site.getImageDataFormat();
					virtualStack = new VirtualStackWithFlexibleLoader( dimensions[ 0 ], dimensions[ 1 ], null, "", imageDataFormat );
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
		imagePlus.setDimensions( 1, nZ, nT );

		return new ImagePlusImageData<>( imagePlus, sharedQueue );
	}
}
