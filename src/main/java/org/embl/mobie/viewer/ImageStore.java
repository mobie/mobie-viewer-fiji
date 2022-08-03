package org.embl.mobie.viewer;

import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.transform.image.AffineTransformedImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class ImageStore
{
	private static Map< String, Image< ? > > images = new ConcurrentHashMap<>();

	public static Image< ? > getImage( String name )
	{
		return images.get( name );
	}

	public static Set< Image< ? > > getImages( Collection< String > names )
	{
		return images.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toSet() );
	}

	// Sort images corresponding to the given names.
	public static List< Image< ? > > getImageList( List< String > names )
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

	public static void putImages( List< ? extends Image< ? > > images )
	{
		for ( Image< ? > image : images )
			ImageStore.images.put( image.getName(), image );
	}
}
