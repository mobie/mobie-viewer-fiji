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
            if (bookmarkFileNames.get(i).equals(bookmarkFileName)) {
                return bookmarkFilesOnGithub.get(i);
            }
        }

        return null;
    }

    public void writeBookmarksToGithub(ArrayList<Bookmark> bookmarks) {
        if (showDialog()) {
            try {
                HashMap<String, Bookmark> namesToBookmarks = new HashMap<>();
                for (Bookmark bookmark : bookmarks) {
                    namesToBookmarks.put(bookmark.name, bookmark);
                }

                Map<String, Bookmark> bookmarksInFile = new HashMap<>();
                ArrayList<String> matchingFilePathsFromGithub = new ArrayList<>();
                String matchingFilePath = getMatchingBookmarkFilePath();
                if (matchingFilePath != null) {
                    matchingFilePathsFromGithub.add(matchingFilePath);
                    Map<String, Bookmark> existingBookmarks = bookmarksJsonParser.parseBookmarks(matchingFilePathsFromGithub);
                    bookmarksInFile.putAll(existingBookmarks);
                }
                bookmarksInFile.putAll(namesToBookmarks);

                final String bookmarkJsonBase64String = bookmarksJsonParser.writeBookmarksToBase64String(bookmarksInFile);
                final GitHubFileCommitter fileCommitter = new GitHubFileCommitter(
                        bookmarkGitLocation.repoUrl, accessToken, bookmarkGitLocation.path + "/" + bookmarkFileName + ".json"
                );
                fileCommitter.commitStringAsFile("Add new bookmarks from UI", bookmarkJsonBase64String);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean showDialog()
    {
        final GenericDialog gd = new GenericDialog( "Save to github" );

        gd.addStringField( "GitHub access token", Prefs.get( ACCESS_TOKEN, "1234567890" ));
        gd.addStringField( "Bookmark file name", "");
        gd.showDialog();

        if ( gd.wasCanceled() ) return false;

        accessToken = gd.getNextString();
        bookmarkFileName = gd.getNextString();

        Prefs.set( ACCESS_TOKEN, accessToken );

        return true;
    }
}
