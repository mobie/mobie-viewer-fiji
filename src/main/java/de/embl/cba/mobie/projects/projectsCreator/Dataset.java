package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagesJsonParser;
import org.apache.commons.compress.utils.FileNameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
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
        this.imagePropertiesMap = new HashMap<>();
        this.defaultBookmarks = new HashMap<>();
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

    public String[] getImageNames() {
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

    public Set<String> getImageNamesInDefaultBookmark() {
        updateDefaultBookmarks();
        return defaultBookmarks.get( "default" ).layers.keySet();
    }

    public Map<String, Bookmark> getDefaultBookmarks() {
        updateDefaultBookmarks();
        return defaultBookmarks;
    }

    public boolean isInDefaultBookmark (String imageName ) {
        return getImageNamesInDefaultBookmark().contains( imageName );
    }

    public String[] getTableNames( String imageName ) {
        File tableFolder = new File( project.getTablesDirectoryPath( name, imageName ) );
        File[] tableFiles = tableFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".tsv");
            }
        });

        if ( !(tableFiles.length > 0) ) {
            return null;
        }

        // we don't include the default table here, as it is always shown
        ArrayList<String> tableNames = new ArrayList<>();
        for (int i = 0; i< tableFiles.length; i++) {
            String tableName = FileNameUtils.getBaseName( tableFiles[i].getAbsolutePath() );
            if (!tableName.equals("default")) {
                tableNames.add( tableName );
            }
        }

        if ( !(tableNames.size() > 0) ) {
            return null;
        }

        String[] tableNamesArray = new String[tableNames.size()];
        tableNamesArray = tableNames.toArray( tableNamesArray );

        return tableNamesArray;
    }




}
