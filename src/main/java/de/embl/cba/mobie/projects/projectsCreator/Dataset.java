package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagesJsonParser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Dataset {

    Project project;
    String name;
    Map<String, ImageProperties> imagePropertiesMap;
    private Map<String, Bookmark> defaultBookmarks;

    public Dataset( Project project, String name ) {
        this.project = project;
        this.name = name;
    }

    private void updateImageProperties() {
        File imagesJSON = new File( project.getImagesJsonPath( name ) );

        if ( imagesJSON.exists() ) {
            imagePropertiesMap = new ImagesJsonParser( project.getDatasetDirectoryPath( name ) ).getImagePropertiesMap();
        } else {
            imagePropertiesMap = new HashMap<>();
        }
    }

    private void updateDefaultBookmarks() {
        File defaultBookmarkJson = new File ( project.getDefaultBookmarkJsonPath( name ) );
        if ( defaultBookmarkJson.exists() ) {
            defaultBookmarks = new BookmarkReader( project.getDatasetDirectoryPath( name ) ).readDefaultBookmarks();
        } else {
            defaultBookmarks = new HashMap<>();
        }
    }

    public ImageProperties getImageProperties( String imageName ) {
        updateImageProperties();
        return imagePropertiesMap.get(imageName);
    }

    public Map<String, ImageProperties> getImagePropertiesMap () {
        updateImageProperties();
        return imagePropertiesMap;
    }

    public String[] getCurrentImageNames() {
        updateImageProperties();
        if ( imagePropertiesMap.size() > 0 ) {
            Set<String> imageNames = imagePropertiesMap.keySet();
            String[] imageNamesArray = new String[imageNames.size()];
            imageNames.toArray( imageNamesArray );
            return imageNamesArray;
        } else {
            return new String[] {""};
        }
    }

    public Set<String> getCurrentImagesInDefaultBookmark() {
        updateDefaultBookmarks();
        return defaultBookmarks.get( "default" ).layers.keySet();
    }

    public Map<String, Bookmark> getDefaultBookmarks() {
        return defaultBookmarks;
    }

    public boolean isInDefaultBookmark (String imageName ) {
        return getCurrentImagesInDefaultBookmark().contains( imageName );
    }


}
