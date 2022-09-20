package org.embl.mobie.viewer;

import org.embl.mobie.viewer.serialize.ImageSource;
import org.embl.mobie.viewer.image.Image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class ImageStore
{
	// Raw data directly opened from a file system or object store.
	// Different views may reuse this data.
	// To make data available to the current view
	// use the {@code fromRawToCurrent} function.
	private static Map< String, Image< ? > > rawData = new ConcurrentHashMap<>();

	// Data that is used by the current view.
	// Contains transformed images as well.
	private static Map< String, Image< ? > > currentData = new ConcurrentHashMap<>();

	public static void putRawData( Image< ? > image )
	{
		rawData.put( image.getName(), image );
	}

	public static boolean isInitialised( String name )
	{
		return rawData.keySet().contains( name );
	}

	public static Set< Image< ? > > getRawData( Collection< String > names )
	{
		return rawData.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toSet() );
	}

	public static Image< ? > getRawData( String name )
	{
		return rawData.get( name );
	}

	public static Image< ? > getImage( String name )
	{
		if ( ! currentData.containsKey( name ) )
			throw new RuntimeException( "Image " + name + " is not part of the current data.");

		return currentData.get( name );
	}

	public static Set< Image< ? > > getImages( Collection< String > names )
	{
		try
		{
			return currentData.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toSet() );
		} catch ( Exception e )
		{
			throw( e );
		}
	}

	// Sort images corresponding to the given names.
	public static List< ? extends Image< ? > > getImageList( List< String > names )
	{
		final ArrayList< Image< ? > > images = new ArrayList<>();
		for ( String name : names )
			images.add( getImage( name ) );
		return images;
	}

	public static void putImage( Image< ? > image )
	{
		currentData.put( image.getName(), image );
	}

	public static void putImages( Collection< ? extends Image< ? > > images )
	{
		for ( Image< ? > image : images )
			ImageStore.currentData.put( image.getName(), image );
	}

	public static void fromRawToCurrent( List< ImageSource > imageSources )
	{
		putImages( getRawData( imageSources.stream().map( s -> s.getName() ).collect( Collectors.toSet()) ) );
	}
}
