package de.embl.cba.mobie.bookmark;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubContentGetter;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class BookmarksJsonParser
{
	private final String datasetLocation;

	public BookmarksJsonParser( String datasetLocation )
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
				entry.getValue().name = entry.getKey();
			}
		}

		return nameToBookmark;
	}

	public void saveBookmarks ( ArrayList<Bookmark> bookmarks) throws IOException {
		HashMap<String, Bookmark> namesToBookmarks = new HashMap<>();
		for (Bookmark bookmark : bookmarks) {
			namesToBookmarks.put(bookmark.name, bookmark);
		}

		String jsonPath = null;
		final JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
		if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			jsonPath = jFileChooser.getSelectedFile().getAbsolutePath();
		}

		if (jsonPath != null) {

			if (!jsonPath.endsWith(".json")) {
				jsonPath += ".json";
			}

			File jsonFile = new File(jsonPath);
			if (jsonFile.exists()) {
				int result = JOptionPane.showConfirmDialog(null,
						"This Json file already exists - append bookmark to this file?", "Append to file?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (result != JOptionPane.YES_OPTION) {
					jsonFile = null;
				}
			}

			if (jsonFile != null) {

				// exclude the name field from json
				ExclusionStrategy strategy = new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						if (f.getName().equals("name")) {
							return true;
						}
						return false;
					}

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						return false;
					}
				};

				Gson gson = new GsonBuilder().addSerializationExclusionStrategy(strategy).create();
				Type type = new TypeToken<Map<String, Bookmark>>() {
				}.getType();

				writeBookmarksToFile(gson, type, jsonFile, namesToBookmarks);
			}
		}

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

	private void writeBookmarksToFile (Gson gson, Type type, File jsonFile, Map< String, Bookmark > bookmarks) throws IOException
	{
		Map<String, Bookmark> bookmarksInFile = new HashMap<>();
		// If json already exists, read existing bookmarks to append new ones
		if (jsonFile.exists()) {
			bookmarksInFile.putAll(readBookmarksFromFile(gson, type, jsonFile.getAbsolutePath()));
		}
		bookmarksInFile.putAll(bookmarks);

		OutputStream outputStream = new FileOutputStream( jsonFile );
		final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8"));
		writer.setIndent("	");
		gson.toJson(bookmarks, type, writer);
		writer.close();
		outputStream.close();
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
