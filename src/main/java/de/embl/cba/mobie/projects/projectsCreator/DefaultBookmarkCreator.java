package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.bookmark.write.BookmarkFileWriter;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultBookmarkCreator {
    String datasetName;
    private Map<String, Bookmark> currentDefaultBookmarks;

    public Set<String> getCurrentImagesInDefaultBookmark(String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        return currentDefaultBookmarks.get( "default" ).layers.keySet();
    }



    public boolean isInDefaultBookmark ( String imageName, String datasetName ) {
        return getCurrentImagesInDefaultBookmark( datasetName ).contains( imageName );
    }

    public void updateCurrentDefaultBookmarks( String datasetName ) {
        File defaultBookmarkJson = new File ( getDefaultBookmarkJsonPath( datasetName ) );
        if ( defaultBookmarkJson.exists() ) {
            currentDefaultBookmarks = new BookmarkReader( getDatasetPath( datasetName ) ).readDefaultBookmarks();
        } else {
            currentDefaultBookmarks = new HashMap<>();
        }
    }

    public void createDefaultBookmark ( String imageName, String datasetName ) {
        updateCurrentImageProperties( datasetName );
        updateCurrentDefaultBookmarks( datasetName );
        HashMap< String, MutableImageProperties> layers = new HashMap<>();
        layers.put( imageName, currentImagesProperties.get(imageName) );

        Bookmark defaultBookmark = new Bookmark();
        defaultBookmark.name = "default";
        defaultBookmark.layers = layers;

        currentDefaultBookmarks.put( "default", defaultBookmark );
    }

    public void addImageToDefaultBookmark( String imageName, String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.put( imageName, currentImagesProperties.get(imageName) );
    }

    public void setImagePropertiesInDefaultBookmark ( String imageName, String datasetName, ImageProperties imageProperties ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.put( imageName, imageProperties );
    }

    public void removeImageFromDefaultBookmark( String imageName, String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.remove( imageName );
    }

    public void writeDefaultBookmarksJson ( String datasetName ) {
        try {
            BookmarkFileWriter.saveBookmarksToFile( currentDefaultBookmarks, new File (getDefaultBookmarkJsonPath( datasetName )) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
