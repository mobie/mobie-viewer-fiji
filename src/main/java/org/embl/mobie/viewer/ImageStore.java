package org.embl.mobie.viewer;

import org.embl.mobie.viewer.source.Image;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class ImageStore
{
	public static Map< String, Image< ? > > images = new ConcurrentHashMap<>();

	public static List< Image< ? > > getImages( List< String > names )
	{
		return images.entrySet().stream().filter( entry -> names.contains( entry.getKey() ) ).map( entry -> entry.getValue() ).collect( Collectors.toList() );
	}

	public static void putImages( List< ? extends Image< ? > > images )
	{
		for ( Image< ? > image : images )
			ImageStore.images.put( image.getName(), image );
	}
}
