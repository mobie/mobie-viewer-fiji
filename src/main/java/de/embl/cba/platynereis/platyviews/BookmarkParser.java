package de.embl.cba.platynereis.platyviews;

import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class BookmarkParser implements Callable< Map< String, Bookmark > >
{
	private final String jsonPath;
	private Map< String, Bookmark > nameToBookmark;

	public BookmarkParser( String jsonPath )
	{
		this.jsonPath = jsonPath;
	}

	public Map< String, Bookmark > call()
	{
		try
		{
			readViewsFromJson( jsonPath );
			return nameToBookmark;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	private void readViewsFromJson( String jsonFilePath ) throws IOException
	{
		final LinkedTreeMap bookmarksTreeMap = Utils.getLinkedTreeMap( jsonFilePath );

		nameToBookmark = new HashMap<>();

		for ( Object bookmarkKey : bookmarksTreeMap.keySet() )
		{
			final Bookmark bookmark = new Bookmark();
			bookmark.name = (String) bookmarkKey ;
			final LinkedTreeMap bookmarkAttributes = ( LinkedTreeMap ) bookmarksTreeMap.get( bookmarkKey );

			addLayers( bookmarkAttributes, bookmark );
			addPositionsAndTransforms( bookmarkAttributes, bookmark );

			nameToBookmark.put( bookmark.name, bookmark );
		}
	}

	private void addPositionsAndTransforms( LinkedTreeMap bookmarkAttributes, Bookmark bookmark )
	{
		final Set keySet = bookmarkAttributes.keySet();
		if ( keySet.contains( "Position" ) )
		{
			bookmark.position = ( ArrayList< Double > ) bookmarkAttributes.get( "Position" );
		}

		if ( keySet.contains( "View" ) )
		{
			bookmark.transform = ( ArrayList< Double >) bookmarkAttributes.get( "View" );
		}
	}

	private void addLayers( LinkedTreeMap bookmarkAttributes, Bookmark bookmark ) throws IOException
	{
		if ( bookmarkAttributes.keySet().contains( "Layers") )
		{
			final LinkedTreeMap layerAttributes = ( LinkedTreeMap ) bookmarkAttributes.get( "Layers" );

			for ( Object layer : layerAttributes.keySet() )
			{
				addImageLayer( (String) layer, ( LinkedTreeMap ) layerAttributes.get( layer ), bookmark );
			}
		}
	}

	private void addImageLayer( String imageId, LinkedTreeMap layerAttributes, Bookmark bookmark )
	{
		final Metadata metadata = new Metadata( imageId );
		metadata.displayName = imageId;

		final Set keySet = layerAttributes.keySet();

		if( keySet.contains( "SelectedIds" ) )
		{
			metadata.selectedSegmentIds = ( ArrayList< Double> ) layerAttributes.get( "SelectedIds" );
		}

		if ( keySet.contains( "Color" ) )
		{
			metadata.displayColor = Utils.getColor( ( String ) layerAttributes.get( "Color" ) );
		}

		if ( keySet.contains( "MaxValue") )
		{
			metadata.displayRangeMax = ( Double ) layerAttributes.get( "MaxValue" );
		}

		if ( keySet.contains( "MinValue") )
		{
			metadata.displayRangeMin = ( Double ) layerAttributes.get( "MinValue" );
		}

		bookmark.imageLayers.add( metadata );
	}

}
