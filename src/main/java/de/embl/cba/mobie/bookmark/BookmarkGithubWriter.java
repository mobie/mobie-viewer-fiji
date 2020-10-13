package de.embl.cba.mobie.bookmark;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubContentGetter;
import de.embl.cba.tables.github.GitHubFileCommitter;
import de.embl.cba.tables.github.GitLocation;
import ij.Prefs;
import ij.gui.GenericDialog;
import org.apache.commons.compress.utils.FileNameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookmarkGithubWriter {

    public static final String ACCESS_TOKEN = "MoBIE.GitHub access token";
    private String accessToken;
    private String bookmarkFileName;
    private GitLocation bookmarkGitLocation;
    private BookmarksJsonParser bookmarksJsonParser;

    BookmarkGithubWriter(GitLocation bookmarkGitLocation, BookmarksJsonParser bookmarksJsonParser) {
        this.bookmarkGitLocation = bookmarkGitLocation;
        this.bookmarksJsonParser = bookmarksJsonParser;
    }

    //repository
    //branch
    //dataset

    private ArrayList< String > getFilePaths()
    {
        final GitHubContentGetter contentGetter =
                new GitHubContentGetter( bookmarkGitLocation.repoUrl, bookmarkGitLocation.path, bookmarkGitLocation.branch, null );
        final String json = contentGetter.getContent();

        GsonBuilder builder = new GsonBuilder();

        final ArrayList< String > bookmarkPaths = new ArrayList<>();
        ArrayList<LinkedTreeMap> linkedTreeMaps = ( ArrayList< LinkedTreeMap >) builder.create().fromJson( json, Object.class );
        for ( LinkedTreeMap linkedTreeMap : linkedTreeMaps )
        {
            final String downloadUrl = ( String ) linkedTreeMap.get( "download_url" );
            bookmarkPaths.add( downloadUrl );
        }
        return bookmarkPaths;
    }

    private ArrayList<String> getBookmarkFileNamesFromPaths (ArrayList<String> bookmarkPaths) {
        ArrayList<String> bookmarkNames = new ArrayList<>();
        for (String path : bookmarkPaths) {
            bookmarkNames.add(FileNameUtils.getBaseName(path));
        }
        return bookmarkNames;
    }

    private String getMatchingBookmarkFilePath () {
        ArrayList<String> bookmarkFilesOnGithub = getFilePaths();
        ArrayList<String> bookmarkFileNames = getBookmarkFileNamesFromPaths(bookmarkFilesOnGithub);
        for (int i=0; i<bookmarkFileNames.size(); i++) {
            if (bookmarkFileNames.get(i) == bookmarkFileName) {
                return bookmarkFilesOnGithub.get(i);
            }
        }

        return null;
    }

    private String constructBookmarkPath () {
        // FileAndUrlUtils.combinePath(bookmarkGitLocation.repoUrl + "/misc/bookmarks");
        return "no";
    }

    private Map<String, Bookmark> appendBookmarkToExistingFile (String githubFilePath, Bookmark bookmark, String bookmarkName) {
        ArrayList<String> bookmarkPaths = new ArrayList<>();
        bookmarkPaths.add(githubFilePath);
        Map<String, Bookmark> bookmarksInFile = new HashMap<>();
        try {
            Map<String, Bookmark> existingBookmarks = bookmarksJsonParser.parseBookmarks(bookmarkPaths);
            bookmarksInFile.putAll(existingBookmarks);
            bookmarksInFile.put(bookmarkName, bookmark);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bookmarksInFile;
    }

    public void writeBookmarkToGithub(Bookmark bookmark) {
        if (showDialog()) {
            // String matchingBookmarkFilePathFromGithub = getMatchingBookmarkFilePath();
            Bookmark bookmarkToWrite = bookmark;

            Map<String, Bookmark> bookmarksInFile = new HashMap<>();
            bookmarksInFile.put(bookmark.name, bookmark);

            final String bookmarkJsonBase64String = bookmarksJsonParser.writeBookmarksToBase64String(bookmarksInFile);
            final GitHubFileCommitter fileCommitter = new GitHubFileCommitter(
                    bookmarkGitLocation.repoUrl, accessToken, bookmarkGitLocation.path + bookmarkFileName + ".json"
            );
            fileCommitter.commitStringAsFile("test bookmark file", bookmarkJsonBase64String);

            // if (matchingBookmarkFilePathFromGithub != null){
            //
            // } else {
            //     //write
            // }
        }
    }

    private boolean showDialog()
    {
        final GenericDialog gd = new GenericDialog( "Save to github" );
        final int columns = 80;

        gd.addStringField( "GitHub access token", Prefs.get( ACCESS_TOKEN, "1234567890" ), columns );
        gd.addStringField( "Bookmark file name", "", columns );
        gd.showDialog();

        if ( gd.wasCanceled() ) return false;

        accessToken = gd.getNextString();
        bookmarkFileName = gd.getNextString();

        Prefs.set( ACCESS_TOKEN, accessToken );

        return true;
    }
}
