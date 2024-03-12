/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FileUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.color.ColorHelper;
import org.janelia.saalfeldlab.n5.Compression;
import org.jetbrains.annotations.NotNull;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;
import static net.imglib2.util.Util.getTypeFromInterval;
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
     * @param imageDataFormat image data format - ImageDataFormat.BdvN5 or ImageDataFormat.OmeZarr
     * @return whether the image exists or not
     */
    public boolean imageExists( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        String filePath =  getDefaultLocalImagePath( datasetName, imageName );
        return new File (filePath).exists();
    }

    public void addImage( ImagePlus imp,
                          String imageName,
                          String datasetName,
                          ImageDataFormat imageDataFormat,
                          ProjectCreator.ImageType imageType,
                          AffineTransform3D sourceTransform,
                          String uiSelectionGroup,
                          boolean exclusive )
    {
        String filePath = getDefaultLocalImagePath( datasetName, imageName );
        File imageFile = new File(filePath);

        if ( ! isImageValid( imp.getNChannels(), imp.getCalibration().getUnit(),
                projectCreator.getVoxelUnit(), false ) )
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
            if (imageType == ProjectCreator.ImageType.image) {
                double[] contrastLimits = new double[]{imp.getDisplayRangeMin(), imp.getDisplayRangeMax()};
                LUT lut = imp.getLuts()[ 0 ];
                String colour = ColorHelper.getString( lut );
                updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup,
                        imageDataFormat, contrastLimits, colour, exclusive, sourceTransform );
            } else {
                updateTableAndJsonsForNewSegmentation(imageName, datasetName, uiSelectionGroup, imageDataFormat, exclusive, sourceTransform );
            }
        }

    }

    @NotNull
    private static OMEZarrWriter.ImageType getImageType( ProjectCreator.ImageType imageType )
    {
        OMEZarrWriter.ImageType type = imageType.equals( ProjectCreator.ImageType.segmentation ) ? OMEZarrWriter.ImageType.Labels : OMEZarrWriter.ImageType.Intensities;
        return type;
    }

    private void writeDefaultImage( ImagePlus imp,
                                    String filePath,
                                    ProjectCreator.ImageType imageType,
                                    String imageName,
                                    ImageDataFormat imageDataFormat )
    {


    }

    private void writeDefaultImage( ImagePlus imp, String filePath,
                             DownsampleBlock.DownsamplingMethod downsamplingMethod,
                             String imageName, ImageDataFormat imageDataFormat,
                             int[][] resolutions, int[][] subdivisions, Compression compression ) {


        // transform only including scaling from image
        AffineTransform3D sourceTransform = ProjectCreatorHelper.generateDefaultAffine( imp );

        switch( imageDataFormat ) {
            case BdvN5:
                new WriteImagePlusToN5().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            case OmeZarr:
                new WriteImagePlusToN5OmeZarr().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression );
                break;

            default:
                throw new UnsupportedOperationException();
        }
    }

    private void deleteImageFiles( String datasetName, String imageName ) throws IOException {

        File zarrFile = new File( getDefaultLocalImagePath( datasetName, imageName ) );
        File tableFile = new File( getDefaultTablePath( datasetName, imageName ) );

        // delete table
        if ( tableFile != null && file.exists() ) {
            Files.delete( file.toPath() );
        }

        // delete image
        if ( zarrFile != null && file.exists() )  {
            FileUtils.deleteDirectory( zarrFile );
        }
    }

    /**
     * Add BigDataViewer (Bdv) format image to MoBIE project e.g. one already in n5/ome-zarr.
     * @param fileLocation image file location - for n5, location of the xml, for ome-zarr,
     *                     location of the .ome.zarr directory.
     * @param imageName image name
     * @param datasetName dataset name
     * @param imageType image type i.e. image or segmentation
     * @param addMethod link, copy or move the image - link (leave image as-is, and link to this location. Only
     *                  supported for N5 and local projects), copy (copy image into project),
     *                  or move (move image into project - be careful as this will delete the image
     *                  from its original location!)
     * @param uiSelectionGroup name of ui selection group to add image view to i.e. the name of the MoBIE dropdown
     *                         menu it will appear in
     * @param imageDataFormat  image data format of image - ImageDataFormat.BdvN5 or ImageDataFormat.OmeZarr
     * @param exclusive whether the image view is exclusive or not i.e. when viewed, does it first remove all current
     *                  images from the viewer?
     * @throws SpimDataException
     * @throws IOException
     */
    public void addBdvFormatImage ( File fileLocation, String imageName, String datasetName, ProjectCreator.ImageType imageType, ProjectCreator.AddMethod addMethod, String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive ) throws SpimDataException, IOException {

        if ( fileLocation.exists() ) {
            SpimData spimData = ( SpimData ) new SpimDataOpener().open( fileLocation.getAbsolutePath(), imageDataFormat );
            addBdvFormatImage( spimData, imageName, datasetName, imageType, addMethod, uiSelectionGroup, imageDataFormat, exclusive );
        } else {
            throw new FileNotFoundException(
                    "Adding image to project failed - " + fileLocation.getAbsolutePath() + " does not exist" );
        }
    }

    /**
     * Add BigDataViewer (Bdv) format image to MoBIE project e.g. one already in n5/ome-zarr.
     * @param spimData spim data object for image
     * @param imageName image name
     * @param datasetName dataset name
     * @param imageType image type i.e. image or segmentation
     * @param addMethod link, copy or move the image - link (leave image as-is, and link to this location. Only
     *                  supported for N5 and local projects), copy (copy image into project),
     *                  or move (move image into project - be careful as this will delete the image
     *                  from its original location!)
     * @param uiSelectionGroup name of ui selection group to add image view to i.e. the name of the MoBIE dropdown
     *                         menu it will appear in
     * @param imageDataFormat image data format of image - ImageDataFormat.BdvN5 or ImageDataFormat.OmeZarr
     * @param exclusive whether the image view is exclusive or not i.e. when viewed, does it first remove all current
     *                  images from the viewer?
     * @throws SpimDataException
     * @throws IOException
     */
    public void addBdvFormatImage ( SpimData spimData, String imageName, String datasetName,
									ProjectCreator.ImageType imageType, ProjectCreator.AddMethod addMethod, String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive ) throws SpimDataException, IOException {

        File imageDirectory = new File( getDefaultLocalImageDirPath( datasetName, imageDataFormat ));

        int nChannels = spimData.getSequenceDescription().getViewSetupsOrdered().size();
        String imageUnit = spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();

        if ( !isImageValid( nChannels, imageUnit, projectCreator.getVoxelUnit(), true ) ) {
            return;
        }

        if ( !isSpimData2D(spimData) && projectCreator.getDataset( datasetName ).is2D() ) {
            throw new UnsupportedOperationException("Can't add a 3D image to a 2D dataset" );
        }

        if ( projectCreator.getVoxelUnit() == null ) {
            projectCreator.setVoxelUnit( imageUnit );
        }

        File newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
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
            case link:
                // Do nothing, the absolute path to the linked image will be added to the dataset.json
                break;
            case copy:
                copyImage( imageDataFormat, spimData, imageDirectory, imageName);
                break;
            case move:
                moveImage( imageDataFormat, spimData, imageDirectory, imageName);
                break;
        }

        if (imageType == ProjectCreator.ImageType.image) {
            updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup, imageDataFormat, new double[]{0.0, 255.0},
                    "white", exclusive, new AffineTransform3D() );
        } else {
            updateTableAndJsonsForNewSegmentation( imageName, datasetName, uiSelectionGroup, imageDataFormat, exclusive, new AffineTransform3D() );
        }

        IJ.log(  imageName + " added to project" );
    }

    private ArrayList<Object[]> makeDefaultTableRowsForTimepoint( Source< ? > labelsSource, int timepoint, boolean addTimepointColumn ) {

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

    // TODO - is this efficient for big images?
    private void addDefaultTableForImage ( String imageName, String datasetName, ImageDataFormat imageDataFormat ) {
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

            ArrayList<Object[]> rows = new ArrayList<>();
            for ( Integer timepoint: spimData.getSequenceDescription().getTimePoints().getTimePoints().keySet() ) {
                rows.addAll( makeDefaultTableRowsForTimepoint( labelsSource, timepoint, hasMultipleTimepoints ) );
            }

            Object[][] rowArray = new Object[rows.size()][columnNames.size()];
            rowArray = rows.toArray(rowArray);

            JTable table = new JTable(rowArray, columnNames.toArray() );
            Tables.saveTable( table, defaultTable );

            IJ.log( "Default table complete" );
        }
    }


    private void updateTableAndJsonsForNewImage ( String imageName, String datasetName, String uiSelectionGroup, ImageDataFormat imageDataFormat, double[] contrastLimits, String colour, boolean exclusive, AffineTransform3D sourceTransform ) {
        DatasetSerializer datasetSerializer = projectCreator.getDatasetJsonCreator();
        datasetSerializer.addImage( imageName, datasetName, uiSelectionGroup,
                imageDataFormat, contrastLimits, colour, exclusive, sourceTransform );
    }

    private void updateTableAndJsonsForNewSegmentation( String imageName, String datasetName, String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive, AffineTransform3D sourceTransform ) {
        addDefaultTableForImage( imageName, datasetName, imageDataFormat );
        DatasetSerializer datasetSerializer = projectCreator.getDatasetJsonCreator();
        datasetSerializer.addSegmentation( imageName, datasetName, uiSelectionGroup, imageDataFormat, exclusive, sourceTransform );
    }

    private void copyImage ( ImageDataFormat imageFormat, SpimData spimData,
                             File imageDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = null;
        File imageLocation = getImageLocationFromSequenceDescription(spimData.getSequenceDescription(), imageFormat );

        switch( imageFormat ) {
            case BdvN5:
                newImageFile = new File(imageDirectory, imageName + ".n5" );
                FileUtils.copyDirectory(imageLocation, newImageFile);
                writeNewBdvXml( spimData, newImageFile, imageDirectory, imageName, imageFormat );
                break;

            case OmeZarr:
                newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
                FileUtils.copyDirectory(imageLocation, newImageFile);
                break;
        }
    }

    private void moveImage ( ImageDataFormat imageFormat, SpimData spimData,
                             File imageDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = null;
        File imageLocation = getImageLocationFromSequenceDescription(spimData.getSequenceDescription(), imageFormat );

        switch( imageFormat ) {
            case BdvN5:
                newImageFile = new File(imageDirectory, imageName + ".n5" );
                // have to explicitly close the image loader, so we can delete the original file
                closeImgLoader( spimData, imageFormat );
                FileUtils.moveDirectory( imageLocation, newImageFile );
                writeNewBdvXml( spimData, newImageFile, imageDirectory, imageName, imageFormat );
                break;

            case OmeZarr:
                newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
                closeImgLoader( spimData, imageFormat );
                FileUtils.moveDirectory( imageLocation, newImageFile );
                break;
        }
    }


    private SpimData fixSetupName( SpimData spimData, String imageName ) {
        // The view setup name must be the same as the image name
        ViewSetup firstSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get(0);
        if ( !firstSetup.getName().equals(imageName) ) {

            int numSetups = spimData.getSequenceDescription().getViewSetups().size();
            final HashMap< Integer, ViewSetup> setups = new HashMap<>( numSetups );
            for ( int s = 0; s < numSetups; s++ )
            {
                final ViewSetup setup;
                if ( s == 0 ) {
                    setup = new ViewSetup( firstSetup.getId(), imageName, firstSetup.getSize(),
                            firstSetup.getVoxelSize(), firstSetup.getChannel(), firstSetup.getAngle(),
                            firstSetup.getIllumination() );
                    for ( Entity attribute: firstSetup.getAttributes().values() ) {
                        setup.setAttribute( attribute );
                    }
                } else {
                    setup = spimData.getSequenceDescription().getViewSetupsOrdered().get( s );
                }
                setups.put( s, setup );
            }

            final SequenceDescription newSeq = new SequenceDescription(
                    spimData.getSequenceDescription().getTimePoints(), setups,
                    spimData.getSequenceDescription().getImgLoader(), null );

            return new SpimData(
                    spimData.getBasePath(), newSeq, spimData.getViewRegistrations() );
        } else {
            return spimData;
        }
    }

    private void writeNewBdvXml ( SpimData spimData, File imageFile, File saveDirectory, String imageName, ImageDataFormat imageFormat ) throws SpimDataException {

        ImgLoader imgLoader = null;
        if (imageFormat == ImageDataFormat.BdvN5) {
            imgLoader = new N5ImageLoader(imageFile, null);
        }

        spimData.setBasePath( saveDirectory );
        spimData.getSequenceDescription().setImgLoader(imgLoader);
        new XmlIoSpimData().save(spimData, new File( saveDirectory, imageName + ".xml").getAbsolutePath() );
    }
}
