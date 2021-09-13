package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.source.*;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.view.View;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.color.ColoringLuts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper.imageFormatToFolderName;

public class DatasetJsonCreator {

    ProjectCreator projectCreator;

    public DatasetJsonCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    public void addToDatasetJson( String imageName, String datasetName, ProjectCreator.ImageType imageType,
                                  String uiSelectionGroup, boolean is2D, int nTimepoints,
                                  ImageDataFormat imageDataFormat ) {
        Dataset dataset = projectCreator.getDataset( datasetName );
        if ( dataset == null ) {
            dataset = new Dataset();
            dataset.sources = new HashMap<>();
            dataset.views = new HashMap<>();
            // start new datasets with is2D as true, then for the first 3D image added it can be set to false
            dataset.is2D = true;
        }

        if ( !is2D ) {
            dataset.is2D = false;
        }

        if ( nTimepoints > dataset.timepoints ) {
            dataset.timepoints = nTimepoints;
        }

        addNewSource( dataset, imageName, imageType, imageDataFormat );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewImageView( dataset, imageName, imageType, uiSelectionGroup );
        }

        // if there is no default view, make one with this image and sensible defaults
        if ( !dataset.views.containsKey("default")) {
            addNewDefaultView( dataset, imageName, imageType );
        }

        writeDatasetJson( datasetName, dataset );
        projectCreator.getDatasetsCreator().addImageDataFormat( imageDataFormat );
    }

    private void addNewSource( Dataset dataset, String imageName, ProjectCreator.ImageType imageType,
                               ImageDataFormat imageDataFormat ) {
        Map<ImageDataFormat, StorageLocation> imageDataLocations;
        SourceSupplier sourceSupplier;

        switch( imageType ) {
            case segmentation:
                SegmentationSource segmentationSource = new SegmentationSource();
                segmentationSource.tableData = new HashMap<>();
                StorageLocation tableStorageLocation = new StorageLocation();
                tableStorageLocation.relativePath = "tables/" + imageName;
                segmentationSource.tableData.put( TableDataFormat.TabDelimitedFile, tableStorageLocation );

                imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
                segmentationSource.imageData = imageDataLocations;

                sourceSupplier = new SourceSupplier( segmentationSource );

                dataset.sources.put( imageName, sourceSupplier );
                break;

            case image:
                ImageSource imageSource = new ImageSource();
                imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
                imageSource.imageData = imageDataLocations;

                sourceSupplier = new SourceSupplier( imageSource );
                dataset.sources.put( imageName, sourceSupplier );
                break;
        }
    }

    private Map<ImageDataFormat, StorageLocation> makeImageDataLocations( ImageDataFormat imageDataFormat,
                                                                          String imageName ) {
        Map<ImageDataFormat, StorageLocation> imageDataLocations = new HashMap<>();
        StorageLocation imageStorageLocation = new StorageLocation();
        if ( imageDataFormat == ImageDataFormat.OmeZarr ) {
            imageStorageLocation.relativePath = "images/" + imageFormatToFolderName( imageDataFormat ) +
                    "/" + imageName + ".ome.zarr";
        } else {
            imageStorageLocation.relativePath = "images/" + imageFormatToFolderName(imageDataFormat) +
                    "/" + imageName + ".xml";
        }
        imageDataLocations.put( imageDataFormat, imageStorageLocation );

        return imageDataLocations;
    }

    private void addNewImageView( Dataset dataset, String imageName, ProjectCreator.ImageType imageType,
                            String uiSelectionGroup ) {

        View view = createView( imageName, imageType, uiSelectionGroup, false );
        dataset.views.put( imageName, view );
    }

    private void addNewDefaultView( Dataset dataset, String imageName, ProjectCreator.ImageType imageType ) {

        View view = createView( imageName, imageType, "bookmark", true );
        dataset.views.put( "default", view );
    }

    private View createView( String imageName, ProjectCreator.ImageType imageType, String uiSelectionGroup,
                             boolean isExclusive ) {
        ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );
        switch( imageType ) {
            case segmentation:
                ArrayList<String> tables = new ArrayList<>();
                tables.add( "default.tsv" );
                SegmentationSourceDisplay segmentationSourceDisplay = new SegmentationSourceDisplay(
                        imageName, 0.5, sources, ColoringLuts.GLASBEY,
                        null, null, null, false,
                        false, new String[]{ TableColumnNames.ANCHOR_X, TableColumnNames.ANCHOR_Y },
                        tables, null, null );
                sourceDisplays.add( segmentationSourceDisplay );
                break;
            case image:
                ImageSourceDisplay imageSourceDisplay = new ImageSourceDisplay( imageName, 1.0, sources,
                        "white", new double[] {0.0, 255.0}, null, false );
                sourceDisplays.add( imageSourceDisplay );
                break;
        }

        View view = new View( uiSelectionGroup, sourceDisplays, null, null, isExclusive );
        return view;
    }

    public void writeDatasetJson ( String datasetName, Dataset dataset ) {
        try {
            String datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, "dataset.json" );
            new DatasetJsonParser().saveDataset( dataset, datasetJsonPath );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // whether the dataset json saving succeeded or not, we reload the current dataset
        try {
            projectCreator.reloadCurrentDataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
