package de.embl.cba.mobie.bookmark;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils.FileLocation;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static de.embl.cba.tables.FileUtils.selectPathFromProjectOrFileSystem;

public class BookmarksJsonParser {
	private final String datasetLocation;

	public BookmarksJsonParser(String datasetLocation) {
		this.datasetLocation = datasetLocation;
	}

	public String getDatasetLocation() {
		return datasetLocation;
	}

	public Map<String, Bookmark> getDefaultBookmarks() {
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

	public Map<String, Bookmark> selectAndLoadBookmarks() {
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

	public void saveBookmarksToGithub(ArrayList<Bookmark> bookmarks) {
		final GitLocation gitLocation = GitHubUtils.rawUrlToGitLocation( datasetLocation );
		gitLocation.path += "misc/bookmarks";

		BookmarkGithubWriter bookmarkWriter = new BookmarkGithubWriter(gitLocation, this);
		bookmarkWriter.writeBookmarksToGithub(bookmarks);
	}

	private Gson createGsonBuilder(boolean usePrettyPrinting) {
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

		Gson gson;
		if (usePrettyPrinting) {
			gson = new GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(strategy).create();
		} else {
			gson = new GsonBuilder().addSerializationExclusionStrategy(strategy).create();
		}
		return gson;
	}

	public void saveBookmarksToFile(ArrayList<Bookmark> bookmarks, FileLocation fileLocation) throws IOException {
		HashMap<String, Bookmark> namesToBookmarks = new HashMap<>();
		for (Bookmark bookmark : bookmarks) {
			namesToBookmarks.put(bookmark.name, bookmark);
		}

		String jsonPath = null;
		final JFileChooser jFileChooser;
		if ( fileLocation == FileLocation.FILE_SYSTEM ) {
			jFileChooser = new JFileChooser();
		} else {
			String bookmarksDirectory = FileAndUrlUtils.combinePath(datasetLocation, "misc", "bookmarks");
			jFileChooser = new JFileChooser(bookmarksDirectory);
		}
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
				if (!appendToFileDialog()) {
					jsonFile = null;
				}
			}

			if (jsonFile != null) {

				Gson gson = createGsonBuilder(false);
				Type type = new TypeToken<Map<String, Bookmark>>() {
				}.getType();

				writeBookmarksToFile(gson, type, jsonFile, namesToBookmarks);
			}
		}

	}

	public boolean appendToFileDialog () {
		int result = JOptionPane.showConfirmDialog(null,
				"This Json file already exists - append bookmark to this file?", "Append to file?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (result != JOptionPane.YES_OPTION) {
			return false;
		} else {
			return true;
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
		gson.toJson(bookmarksInFile, type, writer);
		writer.close();
		outputStream.close();
	}

	public String writeBookmarksToBase64String (Map<String, Bookmark> bookmarks) {
		Gson gson = createGsonBuilder(true);
		Type type = new TypeToken<Map<String, Bookmark>>() {
		}.getType();
		String jsonString = gson.toJson(bookmarks, type);
		byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(jsonBytes);
		// TODO - add new line at end?
	}
}
