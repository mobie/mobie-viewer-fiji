package org.embl.mobie.viewer;

import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.serialize.DataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class DataStore
{
	// Images data directly opened from a file system or object store.
	// Different views may reuse these images.
	// To make images available to the current view
	// use the {@code fromRawToCurrent} function.
	private static Map< String, Image< ? > > rawImages = new ConcurrentHashMap<>();

	// Data that is used by the current view.
	// Contains transformed data as well.
	private static Map< String, Image< ? > > viewImages = new ConcurrentHashMap<>();

	// Other data types that are not necessarily images
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

	public static void putRawImage( Image< ? > image )
	{
		rawImages.put( image.getName(), image );
	}

	public static boolean contains( String name )
	{
		return rawImages.keySet().contains( name );
	}

	public static Set< Image< ? > > getRawImage( Collection< String > names )
	{
		return rawImages.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toSet() );
	}

	public static Image< ? > getRawImage( String name )
	{
		return rawImages.get( name );
	}

	public static Image< ? > getViewImage( String name )
	{
		if ( ! viewImages.containsKey( name ) )
			throw new RuntimeException( "Image " + name + " is not part of the current data.");

		return viewImages.get( name );
	}

	// Unsorted Set.
	public static Set< Image< ? > > getViewImageSet( Collection< String > names )
	{
		try
		{
			return viewImages.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toSet() );
		} catch ( Exception e )
		{
			throw( e );
		}
	}

	// Sorted List, corresponding to the given names.
	public static List< ? extends Image< ? > > getViewImageList( List< String > names )
	{
		final ArrayList< Image< ? > > images = new ArrayList<>();
		for ( String name : names )
			images.add( getViewImage( name ) );
		return images;
	}

	public static void putViewImage( Image< ? > image )
	{
		viewImages.put( image.getName(), image );
	}

	public static void putViewImages( Collection< ? extends Image< ? > > images )
	{
		for ( Image< ? > image : images )
			DataStore.viewImages.put( image.getName(), image );
	}

	// TODO: Why do we need this?
	public static void registerAsViewData( List< DataSource > dataSources )
	{
		putViewImages( getRawImage( dataSources.stream().map( s -> s.getName() ).collect( Collectors.toSet()) ) );
	}
}
