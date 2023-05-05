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
}
