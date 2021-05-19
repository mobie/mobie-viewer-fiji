package de.embl.cba.mobie2.projectcreator;

import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.display.ImageSourceDisplay;
import de.embl.cba.mobie2.display.SegmentationSourceDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.source.SourceSupplier;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.color.ColoringLuts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatasetJsonCreator {

    ProjectCreator projectCreator;

    public DatasetJsonCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    public void addToDatasetJson( String imageName, String datasetName, ProjectCreator.ImageType imageType,
                                  String uiSelectionGroup ) {
        Dataset dataset = projectCreator.getDataset( datasetName );
        addNewSource( dataset, imageName, imageType );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewView( dataset, imageName, imageType, uiSelectionGroup );
        }

        writeDatasetJson( datasetName, dataset );
    }

    private void addNewSource( Dataset dataset, String imageName, ProjectCreator.ImageType imageType ) {
        Map<String, String> imageDataLocations;
        SourceSupplier sourceSupplier;

        switch( imageType ) {
            case segmentation:
                SegmentationSource segmentationSource = new SegmentationSource();
                segmentationSource.tableDataLocation = "tables/" + imageName;
                imageDataLocations = new HashMap<>();
                imageDataLocations.put( "fileSystem", "images/local/" + imageName + ".xml" );
                segmentationSource.imageDataLocations = imageDataLocations;

                sourceSupplier = new SourceSupplier( segmentationSource );

                dataset.sources.put( imageName, sourceSupplier );

            default:
                ImageSource imageSource = new ImageSource();
                imageDataLocations = new HashMap<>();
                imageDataLocations.put( "fileSystem", "images/local/" + imageName + ".xml" );
                imageSource.imageDataLocations = imageDataLocations;

                sourceSupplier = new SourceSupplier( imageSource );

                dataset.sources.put( imageName, sourceSupplier );
        }
    }

    private void addNewView( Dataset dataset, String imageName, ProjectCreator.ImageType imageType,
                            String uiSelectionGroup ) {

        ArrayList<SourceDisplay> sourceDisplays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );
        switch( imageType ) {
            case segmentation:
                SegmentationSourceDisplay segmentationSourceDisplay = new SegmentationSourceDisplay(
                        imageName, 0.5, sources, ColoringLuts.GLASBEY,
                        null, null, null, false,
                        false, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, null );
                sourceDisplays.add( segmentationSourceDisplay );
            default:
                ImageSourceDisplay imageSourceDisplay = new ImageSourceDisplay( imageName, 1.0, sources,
                        "white", new double[] {0.0, 255.0}, null, false );
                sourceDisplays.add( imageSourceDisplay );
        }

        View view = new View( uiSelectionGroup, sourceDisplays, null, null, false );
        dataset.views.put( imageName, view );
    }

    private void writeDatasetJson ( String datasetName, Dataset dataset ) {
        try {
            String datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, "dataset.json" );
            new DatasetJsonParser().saveDataset( dataset, datasetJsonPath );
            projectCreator.reloadCurrentDataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
