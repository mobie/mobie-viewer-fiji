package org.embl.mobie.lib;

import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.DataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DataStore
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	// Images of the current view
	private static Map< String, Image< ? > > images = new ConcurrentHashMap<>();

	// Currently, only used to pre-load tables for region annotations
	private static Map< String, DataSource > rawData = new ConcurrentHashMap<>();

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
}
