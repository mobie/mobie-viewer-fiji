package de.embl.cba.mobie.bookmark.write;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookmarkFileWriter
{
	public static boolean appendToFileDialog ()
	{
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

	public static void writeBookmarksToFile ( Gson gson, Type type, File jsonFile, Map< String, Bookmark > bookmarks) throws IOException
	{
		Map<String, Bookmark> bookmarksInFile = new HashMap<>();
		// If json already exists, read existing bookmarks to append new ones
		if (jsonFile.exists()) {
			bookmarksInFile.putAll( BookmarkReader.readBookmarksFromFile(gson, type, jsonFile.getAbsolutePath()));
		}
		bookmarksInFile.putAll(bookmarks);

		try ( OutputStream outputStream = new FileOutputStream( jsonFile );
			  final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
			writer.setIndent("	");
			gson.toJson(bookmarksInFile, type, writer);
		}
	}

	public static void saveBookmarksToFile( Map<String, Bookmark> bookmarks, File jsonFile ) throws IOException {
		Gson gson = BookmarkGsonBuilderCreator.createGsonBuilder(false);
		Type type = new TypeToken<Map<String, Bookmark>>() {
		}.getType();

		writeBookmarksToFile(gson, type, jsonFile, bookmarks);
	}

	public static String chooseJsonFileLocation(FileUtils.FileLocation fileLocation, String datasetLocation ) {
		String jsonPath = null;
		final JFileChooser jFileChooser;
		if (fileLocation.equals( FileUtils.FileLocation.FileSystem )) {
			jFileChooser = new JFileChooser();
		} else {
			String bookmarksDirectory = FileAndUrlUtils.combinePath( datasetLocation, "misc", "bookmarks");
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
				// check if want to append to existing file, otherwise abort
				if (! appendToFileDialog() ) {
					jsonPath = null;
				}
			}
		}

		return jsonPath;
	}

	public static void saveBookmarksToFile(ArrayList< Bookmark > bookmarks, FileUtils.FileLocation fileLocation, String datasetLocation ) throws IOException {
		HashMap<String, Bookmark> namesToBookmarks = new HashMap<>();
		for (Bookmark bookmark : bookmarks) {
			namesToBookmarks.put(bookmark.name, bookmark);
		}

		String jsonPath = chooseJsonFileLocation( fileLocation, datasetLocation );

		if ( jsonPath != null ) {
			File jsonFile = new File( jsonPath );

			saveBookmarksToFile( namesToBookmarks, jsonFile );
		}
	}
}
