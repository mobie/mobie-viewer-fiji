/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mobie3.viewer.projectcreator;

import de.embl.cba.tables.color.ColoringLuts;
import mobie3.viewer.table.ColumnNames;
import mobie3.viewer.display.ImageDisplay;
import mobie3.viewer.display.SegmentationDisplay;
import mobie3.viewer.display.Display;
import mobie3.viewer.serialize.Dataset;
import mobie3.viewer.serialize.DatasetJsonParser;
import mobie3.viewer.serialize.ImageData;
import mobie3.viewer.serialize.AnnotatedLabelMaskData;
import mobie3.viewer.source.SourceSupplier;
import mobie3.viewer.source.StorageLocation;
import mobie3.viewer.table.TableDataFormat;
import mobie3.viewer.transform.image.AffineTransformation;
import mobie3.viewer.transform.Transformation;
import mobie3.viewer.view.View;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper.imageFormatToFolderName;

public class DatasetJsonCreator {

    ProjectCreator projectCreator;

    public DatasetJsonCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    public void addDataset( String datasetName, boolean is2D ) {
        Dataset dataset = new Dataset();
        dataset.sources = new HashMap<>();
        dataset.views = new HashMap<>();
        dataset.is2D = is2D;
        writeDatasetJson( datasetName, dataset );
    }

    public void addImage(String imageName, String datasetName,
                         String uiSelectionGroup, int nTimepoints,
                         ImageDataFormat imageDataFormat, double[] contrastLimits, String colour,
                         boolean exclusive, AffineTransform3D sourceTransform ) {
        Dataset dataset = fetchDataset( datasetName, nTimepoints );

        addNewImageSource( dataset, imageName, imageDataFormat );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewImageView( dataset, imageName, uiSelectionGroup, contrastLimits, colour, exclusive, sourceTransform );
        }

        // if there is no default view, make one with this image and sensible defaults
        if ( !dataset.views.containsKey("default")) {
            addNewDefaultImageView( dataset, imageName, contrastLimits, colour, sourceTransform );
        }

        writeDatasetJson( datasetName, dataset );
        projectCreator.getProjectJsonCreator().addImageDataFormat( imageDataFormat );
    }

    public void addSegmentation(String imageName, String datasetName, String uiSelectionGroup,
                                int nTimepoints, ImageDataFormat imageDataFormat,
                                boolean exclusive, AffineTransform3D sourceTransform ) {
        Dataset dataset = fetchDataset( datasetName, nTimepoints );

        addNewSegmentationSource( dataset, imageName, imageDataFormat );
        if ( uiSelectionGroup != null ) {
            // add a view with the same name as the image, and sensible defaults
            addNewSegmentationView( dataset, imageName, uiSelectionGroup, exclusive, sourceTransform );
        }

        // if there is no default view, make one with this image and sensible defaults
        if ( !dataset.views.containsKey("default")) {
            addNewDefaultSegmentationView( dataset, imageName, sourceTransform );
        }

        writeDatasetJson( datasetName, dataset );
        projectCreator.getProjectJsonCreator().addImageDataFormat( imageDataFormat );
    }

    public void makeDataset2D( String datasetName, boolean is2D ) {
        Dataset dataset = projectCreator.getDataset( datasetName );
        dataset.is2D = is2D;
        writeDatasetJson( datasetName, dataset );
    }

    private Dataset fetchDataset( String datasetName, int nTimepoints ) {
        Dataset dataset = projectCreator.getDataset( datasetName );

        if ( nTimepoints > dataset.timepoints ) {
            dataset.timepoints = nTimepoints;
        }

        return dataset;
    }

    private void addNewImageSource( Dataset dataset, String imageName, ImageDataFormat imageDataFormat ) {
        Map< ImageDataFormat, StorageLocation > imageDataLocations;
        ImageData imageData = new ImageData();
        imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
        imageData.imageData = imageDataLocations;

        SourceSupplier sourceSupplier = new SourceSupplier( imageData );
        dataset.sources.put( imageName, sourceSupplier );
    }

    private void addNewSegmentationSource( Dataset dataset, String imageName, ImageDataFormat imageDataFormat ) {
        Map< ImageDataFormat, StorageLocation > imageDataLocations;

        AnnotatedLabelMaskData annotatedLabelMaskSource = new AnnotatedLabelMaskData();
        annotatedLabelMaskSource.tableData = new HashMap<>();
        StorageLocation tableStorageLocation = new StorageLocation();
        tableStorageLocation.relativePath = "tables/" + imageName;
        annotatedLabelMaskSource.tableData.put( TableDataFormat.TabDelimitedFile, tableStorageLocation );

        imageDataLocations = makeImageDataLocations( imageDataFormat, imageName );
        annotatedLabelMaskSource.imageData = imageDataLocations;

        SourceSupplier sourceSupplier = new SourceSupplier( annotatedLabelMaskSource );

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
								  double[] contrastLimits, String colour, boolean exclusive,
								  AffineTransform3D sourceTransform ) {
        View view = createImageView( imageName, uiSelectionGroup, exclusive, contrastLimits, colour, sourceTransform );
        dataset.views.put( imageName, view );
    }

    private void addNewSegmentationView( Dataset dataset, String imageName, String uiSelectionGroup, boolean exclusive,
										 AffineTransform3D sourceTransform ) {
        View view = createSegmentationView( imageName, uiSelectionGroup, exclusive, sourceTransform );
        dataset.views.put( imageName, view );
    }

    private void addNewDefaultImageView( Dataset dataset, String imageName, double[] contrastLimits, String colour,
										 AffineTransform3D sourceTransform ) {
        View view = createImageView( imageName, "bookmark", true, contrastLimits, colour,
                sourceTransform );
        dataset.views.put( "default", view );
    }

    private void addNewDefaultSegmentationView( Dataset dataset, String imageName, AffineTransform3D sourceTransform ) {
        View view = createSegmentationView( imageName, "bookmark", true, sourceTransform );
        dataset.views.put( "default", view );
    }

    private List< Transformation > createSourceTransformerList( AffineTransform3D sourceTransform, List<String> sources ) {
        List< Transformation > imageTransformationList = new ArrayList<>();
        Transformation imageTransformation = new AffineTransformation( "affine", sourceTransform.getRowPackedCopy(), sources );
        imageTransformationList.add( imageTransformation );
        return imageTransformationList;
    }
    private View createImageView( String imageName, String uiSelectionGroup, boolean isExclusive,
								  double[] contrastLimits, String colour, AffineTransform3D sourceTransform ) {
        ArrayList< Display > displays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );

        ImageDisplay imageDisplay = new ImageDisplay( imageName, 1.0, sources,
                colour, contrastLimits, null, false );
        displays.add( imageDisplay );

        View view;
        if ( sourceTransform.isIdentity() ) {
            view = new View(uiSelectionGroup, displays, null, null, isExclusive);
        } else {
            List< Transformation > imageTransformationList = createSourceTransformerList( sourceTransform, sources );
            view = new View( uiSelectionGroup, displays, imageTransformationList, null, isExclusive );
        }

        return view;
    }

    private View createSegmentationView( String imageName, String uiSelectionGroup, boolean isExclusive, AffineTransform3D sourceTransform ) {
        ArrayList< Display > displays = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();
        sources.add( imageName );

        ArrayList<String> tables = new ArrayList<>();
        tables.add( "default.tsv" );
        SegmentationDisplay segmentationDisplay = new SegmentationDisplay( imageName, 0.5, sources, ColoringLuts.GLASBEY, null,null, null, false, false, new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y }, tables, null );
        displays.add( segmentationDisplay );

        if ( sourceTransform.isIdentity() ) {
            return new View( uiSelectionGroup, displays, null, null, isExclusive );
        } else {
            List< Transformation > imageTransformationList = createSourceTransformerList( sourceTransform, sources );
            return new View( uiSelectionGroup, displays, imageTransformationList, null, isExclusive );
        }
    }

    public void writeDatasetJson ( String datasetName, Dataset dataset ) {
        try {
            String datasetJsonPath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
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
