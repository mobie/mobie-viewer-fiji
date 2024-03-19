/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.create;

import bdv.viewer.Source;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FileUtils;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.source.SourceHelper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;
import static net.imglib2.util.Util.getTypeFromInterval;
import static net.imglib2.util.Util.round;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.*;

/**
 * Class to add images and segmentations to MoBIE projects in the correct file formats
 */
public class ImagesCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    ProjectCreator projectCreator;

    /**
     * Make an imagesCreator - includes all functions for adding images and segmentations to MoBIE projects
     * @param projectCreator projectCreator
     */
    public ImagesCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private String getDefaultLocalImagePath( String datasetName, String imageName ) {
        return IOHelper.combinePath(getDefaultLocalImageDirPath(datasetName), imageName + ".ome.zarr");
    }

    private String getDefaultLocalImageDirPath( String datasetName ) {
        return IOHelper.combinePath(projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "images" );
    }

    private String getDefaultTableDirPath( String datasetName, String imageName ) {
        return IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "tables", imageName );
    }

    private String getDefaultTablePath( String datasetName, String imageName ) {
        return IOHelper.combinePath( getDefaultTableDirPath(datasetName, imageName), "default.tsv" );
    }

    /**
     * Check if named image exists within dataset
     * @param datasetName dataset name
     * @param imageName image name
     * @return whether the image exists or not
     */
    public boolean imageExists( String datasetName, String imageName ) {
        String filePath =  getDefaultLocalImagePath( datasetName, imageName );
        return new File (filePath).exists();
    }

    public void addImage( ImagePlus imp,
                          String imageName,
                          String datasetName,
                          ProjectCreator.ImageType imageType,
                          AffineTransform3D sourceTransform,
                          String uiSelectionGroup,
                          boolean exclusive )
    {
        String filePath = getDefaultLocalImagePath( datasetName, imageName );
        File imageFile = new File(filePath);

        if ( ! isImageValid( imp.getNChannels(), imp.getCalibration().getUnit(), projectCreator.getVoxelUnit(), false ) )
        {
            return;
        }

        if ( imp.getNDimensions() > 2 && projectCreator.getDataset( datasetName ).is2D() )
        {
            throw new UnsupportedOperationException("Can't add a " + imp.getNDimensions() + "D image to a 2D dataset" );
        }

        if ( projectCreator.getVoxelUnit() == null )
        {
            projectCreator.setVoxelUnit( imp.getCalibration().getUnit() );
        }

        // Done by N5?!
//        File imageDir = new File(imageFile.getParent());
//        if ( ! imageDir.exists() )
//        {
//            imageDir.mkdirs();
//        }


        OMEZarrWriter.write( imp, filePath, getImageType( imageType ), true );

        // check image written successfully, before writing JSONs
        if ( imageFile.exists() ) {
            if (imageType == ProjectCreator.ImageType.Image ) {
                double[] contrastLimits = new double[]{imp.getDisplayRangeMin(), imp.getDisplayRangeMax()};
                LUT lut = imp.getLuts()[ 0 ];
                String colour = ColorHelper.getString( lut );
                updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup, contrastLimits, colour, exclusive, sourceTransform );
            } else {
                updateTableAndJsonsForNewSegmentation(imageName, datasetName, uiSelectionGroup, exclusive, sourceTransform );
            }
        }

    }

    private static OMEZarrWriter.ImageType getImageType( ProjectCreator.ImageType imageType )
    {
        return imageType.equals( ProjectCreator.ImageType.Segmentation ) ? OMEZarrWriter.ImageType.Labels : OMEZarrWriter.ImageType.Intensities;
    }

    private void deleteImageFiles( String datasetName, String imageName )
    {
        try
        {
            File zarrFile = new File( getDefaultLocalImagePath( datasetName, imageName ) );
            File tableFile = new File( getDefaultTablePath( datasetName, imageName ) );

            // delete table
            if ( tableFile != null && tableFile.exists() )
            {
                Files.delete( tableFile.toPath() );
            }

            // delete image
            if ( zarrFile != null && zarrFile.exists() )
            {
                FileUtils.deleteDirectory( zarrFile );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private ArrayList<Object[]> computeObjectFeatures( Source< ? > labelsSource, int timepoint, boolean addTimepointColumn ) {

        RandomAccessibleInterval rai = labelsSource.getSource( timepoint, 0 );

        if ( getTypeFromInterval( rai ) instanceof FloatType ) {
            rai = RealTypeConverters.convert( rai, new IntType() );
        }

        double[] dimensions = new double[ rai.numDimensions() ];
        labelsSource.getVoxelDimensions().dimensions( dimensions );

        ImgLabeling<Integer, IntType> imgLabeling = labelMapAsImgLabeling(rai);

        LabelRegions labelRegions = new LabelRegions(imgLabeling);
        Iterator<LabelRegion> labelRegionIterator = labelRegions.iterator();

        ArrayList<Object[]> rows = new ArrayList<>();
        int nColumns = 10;
        if ( addTimepointColumn ) {
            nColumns += 1;
        }
        while (labelRegionIterator.hasNext()) {
            Object[] row = new Object[ nColumns ];
            LabelRegion labelRegion = labelRegionIterator.next();

            double[] centre = new double[rai.numDimensions()];
            labelRegion.getCenterOfMass().localize(centre);
            double[] bbMin = new double[rai.numDimensions()];
            double[] bbMax = new double[rai.numDimensions()];
            labelRegion.realMin(bbMin);
            labelRegion.realMax(bbMax);

            row[0] = labelRegion.getLabel();
            row[1] = centre[0] * dimensions[0];
            row[2] = centre[1] * dimensions[1];
            row[3] = centre[2] * dimensions[2];
            row[4] = bbMin[0] * dimensions[0];
            row[5] = bbMin[1] * dimensions[1];
            row[6] = bbMin[2] * dimensions[2];
            row[7] = bbMax[0] * dimensions[0];
            row[8] = bbMax[1] * dimensions[1];
            row[9] = bbMax[2] * dimensions[2];

            if ( addTimepointColumn ) {
                row[10] = timepoint;
            }

            rows.add(row);
        }

        return rows;
    }

    public void addOMEZarrImage( String uri,
                                 String imageName,
                                 String datasetName,
                                 ProjectCreator.ImageType imageType,
                                 ProjectCreator.AddMethod addMethod,
                                 String uiSelectionGroup,
                                 boolean exclusive )
    {
        File imagesDirectory = new File( getDefaultLocalImageDirPath( datasetName ) );

        ImageData< ? > imageData = ImageDataOpener.open( uri );

        if ( ! isImageValid( imageData, projectCreator.getVoxelUnit() ) ) {
            return;
        }

        if ( ! is2D( imageData ) && projectCreator.getDataset( datasetName ).is2D() ) {
            // FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/1119
            throw new UnsupportedOperationException("Can't add a 3D image to a 2D dataset" );
        }

        if ( projectCreator.getVoxelUnit() == null ) {
            projectCreator.setVoxelUnit( imageData.getSourcePair( 0 ).getB().getVoxelDimensions().unit() );
        }

        File newImageFile = new File(imagesDirectory, imageName + ".ome.zarr" );
        if ( newImageFile.exists() ) {
            IJ.log("Overwriting image " + imageName + " in dataset " + datasetName );
            deleteImageFiles( datasetName, imageName );
        }

        // make directory for that image, if doesn't exist already
        // FIXME: Do we really need this? N5 seems to be doing this....
//        File imageDir = new File( newImageFile.getParent() );
//        if ( !imageDir.exists() ) {
//            imageDir.mkdirs();
//        }

        switch (addMethod) {
            case Link:
                // Do nothing, the absolute path to the linked image will be added to the dataset.json
                break;
            case Copy:
                copyImage( uri, imagesDirectory, imageName);
                break;
        }

        if ( imageType == ProjectCreator.ImageType.Image ) {
            updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup, new double[]{0.0, 255.0}, "white", exclusive, new AffineTransform3D() );
        } else {
            updateTableAndJsonsForNewSegmentation( imageName, datasetName, uiSelectionGroup, exclusive, new AffineTransform3D() );
        }

        IJ.log(  imageName + " added to project" );
    }

    private void copyImage( String uri, File imagesDir, String imageName )
    {
        try
        {
            File destination = new File( imagesDir, imageName + ".ome.zarr" );
            FileUtils.copyDirectory( new File( uri ), destination);
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void addDefaultTableForImage ( String imageName, String datasetName ) {
        File tableFolder = new File( getDefaultTableDirPath( datasetName, imageName ) );
        File defaultTable = new File( getDefaultTablePath( datasetName, imageName ) );
        if ( !tableFolder.exists() ){
            tableFolder.mkdirs();
        }

        if ( !defaultTable.exists() ) {

            IJ.log( " Creating default table... 0 label is counted as background" );

            // xml file or zarr file, depending on imageDataFormat
            String filePath = getDefaultLocalImagePath( datasetName, imageName );
            ImageData< ? > imageData = ImageDataOpener.open( filePath );
            final Source< ? > labelsSource = imageData.getSourcePair( 0 ).getA();

            boolean hasMultipleTimepoints = labelsSource.isPresent( 1 ); // 0,1,...
            ArrayList<String> columnNames = new ArrayList<>();
            columnNames.add( "label_id" );
            columnNames.add( "anchor_x" );
            columnNames.add( "anchor_y" );
            columnNames.add( "anchor_z" );
            columnNames.add( "bb_min_x" );
            columnNames.add( "bb_min_y" );
            columnNames.add( "bb_min_z" );
            columnNames.add( "bb_max_x" );
            columnNames.add( "bb_max_y" );
            columnNames.add( "bb_max_z" );
            if ( hasMultipleTimepoints ) {
                columnNames.add("timepoint");
            }

            // TODO: if not


            List< Integer > timePoints = SourceHelper.getTimePoints( labelsSource );
            ArrayList<Object[]> rows = new ArrayList<>();
            for ( Integer timePoint : timePoints ) {
                rows.addAll( computeObjectFeatures( labelsSource, timePoint, hasMultipleTimepoints ) );
            }

            Object[][] rowArray = new Object[rows.size()][columnNames.size()];
            rowArray = rows.toArray(rowArray);

            JTable table = new JTable(rowArray, columnNames.toArray() );
            Tables.saveTable( table, defaultTable );

            IJ.log( "Default object features have been computed." );
        }
    }


    private void updateTableAndJsonsForNewImage ( String imageName, String datasetName, String uiSelectionGroup, double[] contrastLimits, String colour, boolean exclusive, AffineTransform3D sourceTransform ) {
        DatasetSerializer datasetSerializer = projectCreator.getDatasetJsonCreator();
        datasetSerializer.addImage( imageName, datasetName, uiSelectionGroup, contrastLimits, colour, exclusive, sourceTransform );
    }

    private void updateTableAndJsonsForNewSegmentation( String imageName, String datasetName, String uiSelectionGroup, boolean exclusive, AffineTransform3D sourceTransform ) {
        addDefaultTableForImage( imageName, datasetName );
        DatasetSerializer datasetSerializer = projectCreator.getDatasetJsonCreator();
        datasetSerializer.addSegmentation( imageName, datasetName, uiSelectionGroup, exclusive, sourceTransform );
    }
}
