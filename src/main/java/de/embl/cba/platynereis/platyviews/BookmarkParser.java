package de.embl.cba.platynereis.platyviews;

import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.platysources.PlatyBrowserImageSourcesModel;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.image.ImageSourcesModel;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class BookmarkParser implements Callable< Map< String, Bookmark > >
{
	private final String jsonPath;
	private final PlatyBrowserImageSourcesModel imageSourcesModel;
	private Map< String, Bookmark > nameToBookmark;

	public BookmarkParser( String jsonPath, PlatyBrowserImageSourcesModel imageSourcesModel )
	{
		this.jsonPath = jsonPath;
		this.imageSourcesModel = imageSourcesModel;
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

		nameToBookmark = new TreeMap<>();

		for ( Object bookmarkKey : bookmarksTreeMap.keySet() )
		{
			final Bookmark bookmark = new Bookmark();
			bookmark.name = (String) bookmarkKey ;
			final LinkedTreeMap bookmarkAttributes = ( LinkedTreeMap ) bookmarksTreeMap.get( bookmarkKey );

			addImageLayers( bookmarkAttributes, bookmark );
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

	private void addImageLayers( LinkedTreeMap bookmarkAttributes, Bookmark bookmark )
	{
		if ( bookmarkAttributes.keySet().contains( "Layers") )
		{
			final LinkedTreeMap imageLayers = ( LinkedTreeMap ) bookmarkAttributes.get( "Layers" );

			for ( Object imageId : imageLayers.keySet() )
			{
				final LinkedTreeMap imageAttributes = ( LinkedTreeMap ) imageLayers.get( imageId );
				final Metadata metadata = imageSourcesModel.getMetadata( ( String ) imageId, imageAttributes );
				bookmark.nameToMetadata.put( metadata.displayName, metadata );
			}
		}
	}
}
