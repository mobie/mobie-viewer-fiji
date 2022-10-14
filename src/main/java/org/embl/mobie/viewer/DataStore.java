package org.embl.mobie.viewer;

import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.serialize.DataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DataStore
{
	// Images of the current view
	private static Map< String, Image< ? > > images = new ConcurrentHashMap<>();

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
			throw new RuntimeException( "Image " + name + " is not part of the current data.");

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
		// FIXME
		//   https://github.com/mobie/mobie-viewer-fiji/issues/813
		images.clear();
	}
}
