package de.embl.cba.platynereis.platyviews;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.github.RepoUrlAndPath;
import de.embl.cba.platynereis.platysources.PlatyBrowserImageSourcesModel;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubContentGetter;
import org.jfree.data.json.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;

public class BookmarkParser implements Callable< Map< String, Bookmark > >
{
	private final ArrayList< String > bookmarksLocations = new ArrayList<>(  );
	private final PlatyBrowserImageSourcesModel imageSourcesModel;
	private Map< String, Bookmark > nameToBookmark;

	public BookmarkParser( String datasetLocation, PlatyBrowserImageSourcesModel imageSourcesModel )
	{
		this.imageSourcesModel = imageSourcesModel;

		try
		{
			addBookmarksFromSingleFile( datasetLocation );
		}
		catch ( Exception e )
		{
			if ( datasetLocation.contains( "githubusercontent" ) )
			{
				addBookmarkFilesFromGithub( datasetLocation );
			}
			else
			{
				addBookmarkFilesFromFolder( datasetLocation );
			}
		}
	}

	public void addBookmarksFromSingleFile( String datasetLocation ) throws IOException
	{
		final String bookmarksFileLocation = FileAndUrlUtils.combinePath( datasetLocation + "/misc/bookmarks.json" );
		FileAndUrlUtils.getInputStream( bookmarksFileLocation ); // to throw an error if not found

		bookmarksLocations.add( bookmarksFileLocation );
	}

	public void addBookmarkFilesFromFolder( String datasetLocation )
	{
		final String bookmarksFolder = FileAndUrlUtils.combinePath( datasetLocation + "/misc/bookmarks" );
		final List< File > fileList = FileUtils.getFileList( new File( bookmarksFolder ), ".*.json", false );

		for ( File file : fileList )
		{
			bookmarksLocations.add( file.getAbsolutePath() );
		}
	}

	public void addBookmarkFilesFromGithub( String datasetLocation )
	{
		final RepoUrlAndPath repoUrlAndPath = rawUrlToRepoUrlAndPath( datasetLocation );
		repoUrlAndPath.path += "misc/bookmarks";

		final GitHubContentGetter contentGetter = new GitHubContentGetter( repoUrlAndPath.repoUrl, repoUrlAndPath.path );
		final String json = contentGetter.getContent();

		GsonBuilder builder = new GsonBuilder();
		ArrayList< LinkedTreeMap > linkedTreeMaps = ( ArrayList< LinkedTreeMap >) builder.create().fromJson( json, Object.class );
		for ( LinkedTreeMap linkedTreeMap : linkedTreeMaps )
		{
			final String downloadUrl = ( String ) linkedTreeMap.get( "download_url" );
			bookmarksLocations.add( downloadUrl );
		}
	}

	public static RepoUrlAndPath rawUrlToRepoUrlAndPath( String datasetLocation )
	{
		final RepoUrlAndPath repoUrlAndPath = new RepoUrlAndPath();
		final String[] split = datasetLocation.split( "/" );
		final String user = split[ 3 ];
		final String repo = split[ 4 ];
		repoUrlAndPath.repoUrl = "https://github.com/" + user + "/" + repo;
		repoUrlAndPath.path = "";
		for ( int i = 6; i < split.length; i++ )
		{
			repoUrlAndPath.path += split[ i ] + "/";
		}
		return repoUrlAndPath;
	}

	public Map< String, Bookmark > call()
	{
		try
		{
			readBookmarks();
			return nameToBookmark;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	private void readBookmarks() throws IOException
	{
		nameToBookmark = new TreeMap<>();

		for ( String bookmarksLocation : bookmarksLocations )
		{
			InputStream bookmarksStream = FileAndUrlUtils.getInputStream( bookmarksLocation );

			final LinkedTreeMap bookmarksTreeMap = Utils.getLinkedTreeMap( bookmarksStream );

			for ( Object bookmarkKey : bookmarksTreeMap.keySet() )
			{
				final Bookmark bookmark = new Bookmark();
				bookmark.name = ( String ) bookmarkKey;
				final LinkedTreeMap bookmarkAttributes = ( LinkedTreeMap ) bookmarksTreeMap.get( bookmarkKey );

				addImageLayers( bookmarkAttributes, bookmark );
				addPositionsAndTransforms( bookmarkAttributes, bookmark );

				nameToBookmark.put( bookmark.name, bookmark );
			}
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
