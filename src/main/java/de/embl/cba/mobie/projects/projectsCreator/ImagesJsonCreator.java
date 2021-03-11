package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagesJsonParser;
import de.embl.cba.mobie.image.Storage;
import de.embl.cba.tables.color.ColoringLuts;

import java.io.IOException;
import java.util.Map;

public class ImagesJsonCreator {

    Project project;

    public ImagesJsonCreator( Project project ) {
        this.project = project;
    }

    public void addToImagesJson (String imageName, String datasetName, ProjectsCreator.ImageType imageType ) {

        Map<String, ImageProperties> currentImageProperties = project.getDataset( datasetName ).getImagePropertiesMap();
        ImageProperties newImageProperties = new ImageProperties();
        newImageProperties.type = imageType.toString();

        switch( imageType ) {
            case segmentation:
                newImageProperties.color = ColoringLuts.GLASBEY;
                newImageProperties.tableFolder = "tables/" + imageName;
                break;
            default:
                newImageProperties.color = "white";
        }

        newImageProperties.contrastLimits = new double[] {0, 255};

        Storage storage = new Storage();
        storage.local = "local/" + imageName + ".xml";
        newImageProperties.storage = storage;

        currentImageProperties.put( imageName, newImageProperties);

        writeImagesJson( datasetName, currentImageProperties );
    }

    public void setImagePropertiesInImagesJson( String imageName, String datasetName, ImageProperties imageProperties ) {
        Map<String, ImageProperties> currentImageProperties = project.getDataset( datasetName ).getImagePropertiesMap();
        currentImageProperties.put( imageName, imageProperties );
        writeImagesJson( datasetName, currentImageProperties );
    }

    private void writeImagesJson ( String datasetName, Map<String, ImageProperties> imagePropertiesMap ) {
        try {
            new ImagesJsonParser( project.getDatasetDirectoryPath( datasetName) ).writeImagePropertiesMap(
                    project.getImagesJsonPath( datasetName),
                    imagePropertiesMap );
            project.getDataset( datasetName ).updateImageProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
