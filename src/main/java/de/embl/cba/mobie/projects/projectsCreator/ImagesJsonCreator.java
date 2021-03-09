package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagesJsonParser;
import de.embl.cba.mobie.image.Storage;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.color.ColoringLuts;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImagesJsonCreator {

    String datasetName;
    Map<String, ImageProperties> currentImageProperties;

    public ImagesJsonCreator( String datasetName ) {
        this.datasetname = datasetName;
    }

    public Map<String, ImageProperties> getCurrentImageProperties() {
        updateCurrentImageProperties();
        return currentImageProperties;
    }

    private void updateCurrentImageProperties() {
        File imagesJSON = new File( getImagesJsonPath( datasetName ) );

        if ( imagesJSON.exists() ) {
            currentImageProperties = new ImagesJsonParser( getDatasetPath( datasetName ) ).getImagePropertiesMap();
        } else {
            currentImageProperties = new HashMap<>();
        }
    }




    public void addToImagesJson (String imageName, ProjectsCreator.ImageType imageType, String datasetName ) {
        updateCurrentImageProperties( datasetName );
        ImageProperties newImageProperties = new ImageProperties();
        newImageProperties.type = imageType.toString();

        switch( imageType ) {
            case segmentation:
                newImageProperties.color = ColoringLuts.GLASBEY;
                newImageProperties.tableFolder = "tables/" + imageName;
                addDefaultTableForImage( imageName, datasetName );
                break;
            default:
                newImageProperties.color = "white";
        }

        newImageProperties.contrastLimits = new double[] {0, 255};

        Storage storage = new Storage();
        storage.local = "local/" + imageName + ".xml";
        newImageProperties.storage = storage;

        currentImageProperties.put( imageName, newImageProperties);

        writeImagesJson( datasetName );
    }

    public void writeImagesJson ( String datasetName ) {
        try {
            new ImagesJsonParser( getDatasetPath( datasetName) ).writeImagePropertiesMap( getImagesJsonPath( datasetName),
                    currentImageProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImageProperties getImageProperties ( String datasetName, String imageName ) {
        updateCurrentImageProperties( datasetName );
        return currentImageProperties.get( imageName );
    }

    private void updateJsonsForNewImage (String imageName, ProjectsCreator.ImageType imageType, String datasetName ) {
        // update images.json
        addToImagesJson(imageName, imageType, datasetName);

        // if there's no default json, create one with this image
        File defaultBookmarkJson = new File(getDefaultBookmarkJsonPath(datasetName));
        if (!defaultBookmarkJson.exists()) {
            createDefaultBookmark(imageName, datasetName);
            writeDefaultBookmarksJson(datasetName);
        }
    }
}
