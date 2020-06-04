package de.embl.cba.mobie.bookmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubContentGetter;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BookmarksJsonParser
{
	private final String datasetLocation;

	public BookmarksJsonParser( String datasetLocation, SourcesModel imageSourcesModel )
	{
		this.datasetLocation = datasetLocation;
	}

	public Map< String, Bookmark > getBookmarks()
	{
		try
		{
			return readBookmarks();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private ArrayList< String > fetchBookmarkPaths( String datasetLocation ) throws IOException
	{
		final ArrayList< String > bookmarkPaths = new ArrayList<>();

		final String bookmarksFileLocation = FileAndUrlUtils.combinePath( datasetLocation + "/misc/bookmarks.json" );
		FileAndUrlUtils.getInputStream( bookmarksFileLocation ); // to throw an error if not found
		bookmarkPaths.add( bookmarksFileLocation );

		return bookmarkPaths;
	}

	private ArrayList< String > addBookmarkFilesFromFolder( String datasetLocation )
	{
		final ArrayList< String > bookmarkPaths = new ArrayList<>();

		final String bookmarksFolder = FileAndUrlUtils.combinePath( datasetLocation + "/misc/bookmarks" );
		final List< File > fileList = FileUtils.getFileList( new File( bookmarksFolder ), ".*.json", false );

		for ( File file : fileList )
		{
			bookmarkPaths.add( file.getAbsolutePath() );
		}

		return bookmarkPaths;
	}

	private ArrayList< String > addBookmarkFilesFromGithub( String datasetLocation )
	{
		final GitLocation gitLocation = GitHubUtils.rawUrlToGitLocation( datasetLocation );
		gitLocation.path += "misc/bookmarks";

		final ArrayList< String > bookmarkPaths = getFilePaths( gitLocation );

		return bookmarkPaths;
	}

	public static ArrayList< String > getFilePaths( GitLocation gitLocation )
	{
		final GitHubContentGetter contentGetter = new GitHubContentGetter( gitLocation.repoUrl, gitLocation.path, gitLocation.branch, null );
		final String json = contentGetter.getContent();

		GsonBuilder builder = new GsonBuilder();

		final ArrayList< String > bookmarkPaths = new ArrayList<>();
		ArrayList< LinkedTreeMap > linkedTreeMaps = ( ArrayList< LinkedTreeMap >) builder.create().fromJson( json, Object.class );
		for ( LinkedTreeMap linkedTreeMap : linkedTreeMaps )
		{
			final String downloadUrl = ( String ) linkedTreeMap.get( "download_url" );
			bookmarkPaths.add( downloadUrl );
		}
		return bookmarkPaths;
	}

	private Map< String, Bookmark > readBookmarks() throws IOException
	{
		final ArrayList< String > bookmarkFiles = fetchBookmarkPaths();
		final Map< String, Bookmark > nameToBookmark = parseBookmarks( bookmarkFiles );
		return nameToBookmark;
	}

	private Map< String, Bookmark > parseBookmarks( ArrayList< String > bookmarksFiles ) throws IOException
	{
		Map< String, Bookmark > nameToBookmark = new TreeMap<>();

		Gson gson = new Gson();
		Type type = new TypeToken< Map< String, Bookmark > >() {}.getType();

		// parse bookmark files
		// each file can contain multiple bookmarks
		for ( String bookmarksFile : bookmarksFiles )
		{
			if ( ! bookmarksFile.endsWith( ".json" ) )
			{
				System.out.println("Found invalid bookmarks file: " + bookmarksFile );
				continue;
			}

			final Map< String, Bookmark > bookmarks = readBookmarksFromFile( gson, type, bookmarksFile );

			for ( Map.Entry< String, Bookmark > entry : bookmarks.entrySet() )
			{
				nameToBookmark.put( entry.getKey(), entry.getValue() );
			}
		}

		return nameToBookmark;
	}

	private Map< String, Bookmark > readBookmarksFromFile( Gson gson, Type type, String bookmarksLocation ) throws IOException
	{
		InputStream inputStream = FileAndUrlUtils.getInputStream( bookmarksLocation );
		final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" ) );
		final Map< String, Bookmark > stringBookmarkMap = gson.fromJson( reader, type );
		reader.close();
		inputStream.close();
		return stringBookmarkMap;
	}

	private ArrayList< String > fetchBookmarkPaths()
	{
		try
		{
			return fetchBookmarkPaths( datasetLocation );
		}
		catch ( Exception e )
		{
			if ( datasetLocation.contains( "githubusercontent" ) )
			{
				return addBookmarkFilesFromGithub( datasetLocation );
			}
			else
			{
				return addBookmarkFilesFromFolder( datasetLocation );
			}
		}
	}

}
