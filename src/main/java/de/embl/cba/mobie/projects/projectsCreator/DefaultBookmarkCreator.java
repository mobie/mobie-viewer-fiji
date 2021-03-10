package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.bookmark.write.BookmarkFileWriter;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultBookmarkCreator {

    Project project;

    public DefaultBookmarkCreator( Project project ) {
        this.project = project;
    }

    public void createDefaultBookmark ( String imageName, String datasetName ) {
        File defaultBookmarkJson = new File( project.getDefaultBookmarkJsonPath(datasetName) );
        if ( !defaultBookmarkJson.exists() ) {
            HashMap<String, MutableImageProperties> layers = new HashMap<>();
            layers.put(imageName, project.getDataset(datasetName).getImageProperties(imageName));

            Bookmark defaultBookmark = new Bookmark();
            defaultBookmark.name = "default";
            defaultBookmark.layers = layers;

            Map<String, Bookmark> defaultBookmarks = project.getDataset(datasetName).getDefaultBookmarks();
            defaultBookmarks.put("default", defaultBookmark);
            writeDefaultBookmarksJson( datasetName, defaultBookmarks );
        }
    }

    public void addImageToDefaultBookmark( String imageName, String datasetName ) {
        Map<String, Bookmark> defaultBookmarks = project.getDataset(datasetName).getDefaultBookmarks();
        defaultBookmarks.get( "default" ).layers.put( imageName, project.getDataset( datasetName ).getImageProperties( imageName) );
        writeDefaultBookmarksJson( datasetName, defaultBookmarks );
    }

    public void setImagePropertiesInDefaultBookmark ( String imageName, String datasetName,
                                                      ImageProperties imageProperties ) {
        Map<String, Bookmark> defaultBookmarks = project.getDataset(datasetName).getDefaultBookmarks();
        defaultBookmarks.get( "default" ).layers.put( imageName, imageProperties );
        writeDefaultBookmarksJson( datasetName, defaultBookmarks );
    }

    public void removeImageFromDefaultBookmark( String imageName, String datasetName ) {
        Set<String> currentImages = project.getDataset(datasetName).getCurrentImagesInDefaultBookmark();
        if ( currentImages.size() > 1 ) {
            Map<String, Bookmark> defaultBookmarks = project.getDataset(datasetName).getDefaultBookmarks();
            defaultBookmarks.get( "default" ).layers.remove( imageName );
            writeDefaultBookmarksJson( datasetName, defaultBookmarks );
        } else {
            Utils.log( "can't make image non-default - you need at least one default image" );
        }

    }

    private void writeDefaultBookmarksJson ( String datasetName, Map<String, Bookmark> defaultBookmarks ) {
        try {
            BookmarkFileWriter.saveBookmarksToFile( defaultBookmarks,
                    new File ( project.getDefaultBookmarkJsonPath( datasetName )) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
