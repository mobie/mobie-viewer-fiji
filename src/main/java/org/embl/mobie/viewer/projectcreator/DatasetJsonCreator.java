package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.source.*;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.io.util.FileAndUrlUtils;
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

    public void addImageToDatasetJson( String imageName, String datasetName,
                                       String uiSelectionGroup, boolean is2D, int nTimepoints,
                                       ImageDataFormat imageDataFormat, double[] contrastLimits, String colour ) {
        Dataset dataset = fetchDataset( datasetName, is2D, nTimepoints );

        addNewImageSource( dataset, imageName, imageDataFormat );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewImageView( dataset, imageName, uiSelectionGroup, contrastLimits, colour );
        }

        // if there is no default view, make one with this image and sensible defaults
        if ( !dataset.views.containsKey("default")) {
            addNewDefaultImageView( dataset, imageName, contrastLimits, colour );
        }

        writeDatasetJson( datasetName, dataset );
        projectCreator.getDatasetsCreator().addImageDataFormat( imageDataFormat );
    }

    public void addSegmentationToDatasetJson( String imageName, String datasetName, String uiSelectionGroup,
                                              boolean is2D, int nTimepoints, ImageDataFormat imageDataFormat ) {
        Dataset dataset = fetchDataset( datasetName, is2D, nTimepoints );

        addNewSegmentationSource( dataset, imageName, imageDataFormat );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewSegmentationView( dataset, imageName, uiSelectionGroup );
        }

        // if there is no default view, make one with this image and sensible defaults
        if ( !dataset.views.containsKey("default")) {
            addNewDefaultSegmentationView( dataset, imageName );
        }

        writeDatasetJson( datasetName, dataset );
        projectCreator.getDatasetsCreator().addImageDataFormat( imageDataFormat );
    }

    private Dataset fetchDataset( String datasetName, boolean is2D, int nTimepoints ) {
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

        return dataset;
    }

    private void addNewImageSource( Dataset dataset, String imageName, ImageDataFormat imageDataFormat ) {
        Map< ImageDataFormat, StorageLocation > imageDataLocations;
        ImageSource imageSource = new ImageSource();
        imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
        imageSource.imageData = imageDataLocations;

        SourceSupplier sourceSupplier = new SourceSupplier( imageSource );
        dataset.sources.put( imageName, sourceSupplier );
    }

    private void addNewSegmentationSource( Dataset dataset, String imageName, ImageDataFormat imageDataFormat ) {
        Map< ImageDataFormat, StorageLocation > imageDataLocations;

        SegmentationSource segmentationSource = new SegmentationSource();
        segmentationSource.tableData = new HashMap<>();
        StorageLocation tableStorageLocation = new StorageLocation();
        tableStorageLocation.relativePath = "tables/" + imageName;
        segmentationSource.tableData.put( TableDataFormat.TabDelimitedFile, tableStorageLocation );

        imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
        segmentationSource.imageData = imageDataLocations;

        SourceSupplier sourceSupplier = new SourceSupplier( segmentationSource );

        dataset.sources.put( imageName, sourceSupplier );
    }

    private Map< ImageDataFormat, StorageLocation > makeImageDataLocations( ImageDataFormat imageDataFormat,
                                                                            String imageName ) {
        Map< ImageDataFormat, StorageLocation > imageDataLocations = new HashMap<>();
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

    private void addNewImageView( Dataset dataset, String imageName, String uiSelectionGroup,
                                  double[] contrastLimits, String colour ) {
        View view = createImageView( imageName, uiSelectionGroup, false, contrastLimits, colour );
        dataset.views.put( imageName, view );
    }

    private void addNewSegmentationView( Dataset dataset, String imageName, String uiSelectionGroup ) {
        View view = createSegmentationView( imageName, uiSelectionGroup, false );
        dataset.views.put( imageName, view );
    }

    private void addNewDefaultImageView( Dataset dataset, String imageName, double[] contrastLimits, String colour ) {
        View view = createImageView( imageName, "bookmark", true, contrastLimits, colour );
        dataset.views.put( "default", view );
    }

    private void addNewDefaultSegmentationView( Dataset dataset, String imageName ) {
        View view = createSegmentationView( imageName, "bookmark", true );
        dataset.views.put( "default", view );
    }

    private View createImageView( String imageName, String uiSelectionGroup, boolean isExclusive,
                                  double[] contrastLimits, String colour ) {
        ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );

        ImageSourceDisplay imageSourceDisplay = new ImageSourceDisplay( imageName, 1.0, sources,
                colour, contrastLimits, null, false );
        sourceDisplays.add( imageSourceDisplay );

        View view = new View( uiSelectionGroup, sourceDisplays, null, null, isExclusive );
        return view;
    }

    private View createSegmentationView( String imageName, String uiSelectionGroup, boolean isExclusive ) {
        ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );

        ArrayList<String> tables = new ArrayList<>();
        tables.add( "default.tsv" );
        SegmentationSourceDisplay segmentationSourceDisplay = new SegmentationSourceDisplay(
                imageName, 0.5, sources, ColoringLuts.GLASBEY,
                null, null, null, false,
                false, new String[]{ TableColumnNames.ANCHOR_X, TableColumnNames.ANCHOR_Y },
                tables, null, null );
        sourceDisplays.add( segmentationSourceDisplay );

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
