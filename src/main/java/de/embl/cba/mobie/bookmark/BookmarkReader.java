package de.embl.cba.mobie.bookmark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

import static de.embl.cba.tables.FileUtils.selectPathFromProjectOrFileSystem;

public class BookmarkReader
{
	private final String datasetLocation;

	public BookmarkReader( String datasetLocation ) {
		this.datasetLocation = datasetLocation;
	}

	public static Map< String, Bookmark > readBookmarksFromFile( Gson gson, Type type, String bookmarksLocation ) throws IOException
	{
		try ( InputStream inputStream = FileAndUrlUtils.getInputStream( bookmarksLocation );
			  final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" )) ) {
			final Map<String, Bookmark> stringBookmarkMap = gson.fromJson(reader, type);
			return stringBookmarkMap;
		}
	}

	public String getDatasetLocation() {
		return datasetLocation;
	}

	public Map<String, Bookmark> readDefaultBookmarks() {
		try {
			ArrayList<String> filePaths = new ArrayList<>();
			String bookmarkPath = FileAndUrlUtils.combinePath(datasetLocation, "misc", "bookmarks", "default.json");
			filePaths.add(bookmarkPath);
			return parseBookmarks( filePaths );
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, Bookmark> selectAndReadBookmarks() {
		try {
			ArrayList<String> filePaths = new ArrayList<>();
			String bookmarksDirectory = FileAndUrlUtils.combinePath(datasetLocation, "misc", "bookmarks");
			String selectedFilePath = selectPathFromProjectOrFileSystem( bookmarksDirectory, "Bookmark" );
			if (selectedFilePath != null) {
				filePaths.add(selectedFilePath);
				return parseBookmarks(filePaths);
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map< String, Bookmark > parseBookmarks( ArrayList< String > bookmarksFiles ) throws IOException
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
				entry.getValue().name = entry.getKey();
			}
		}

		return nameToBookmark;
	}

}
