package de.embl.cba.mobie.bookmark.write;

import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;
import ij.gui.GenericDialog;

import java.util.ArrayList;

public class BookmarkWriter
{
	public static void saveBookmarksToGithub( ArrayList< Bookmark > bookmarks, BookmarkReader bookmarkReader ) {
		final GitLocation gitLocation = GitHubUtils.rawUrlToGitLocation( bookmarkReader.getDatasetLocation() );
		gitLocation.path += "misc/bookmarks";

		BookmarkGithubWriter bookmarkWriter = new BookmarkGithubWriter(gitLocation, bookmarkReader );
		bookmarkWriter.writeBookmarksToGithub(bookmarks);
	}

	public static NameAndFileLocation bookmarkSaveDialog () {
		FileUtils.FileLocation fileLocation = null;
		String bookmarkName = null;
		final GenericDialog gd = new GenericDialog( "Choose save location" );
		gd.addStringField("Bookmark Name", "name");
		gd.addChoice( "Save to", new String[]{ FileUtils.FileLocation.Project.toString(),
				FileUtils.FileLocation.FileSystem.toString() }, FileUtils.FileLocation.Project.toString() );
		gd.showDialog();

		if ( gd.wasCanceled() ) return null;
		bookmarkName = gd.getNextString();
		fileLocation = FileUtils.FileLocation.valueOf( gd.getNextChoice() );

		NameAndFileLocation bookmarkNameAndFileLocation = new NameAndFileLocation();
		bookmarkNameAndFileLocation.name = bookmarkName;
		bookmarkNameAndFileLocation.location = fileLocation;

		return bookmarkNameAndFileLocation;
	}
}
