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
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
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
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.n5.loaders.N5FSImageLoader;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.n5.writers.WriteImagePlusToN5;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.writers.imageplus.WriteImagePlusToN5OmeZarr;
import org.embl.mobie.io.util.IOHelper;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
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
import static org.embl.mobie.lib.create.ProjectCreatorHelper.getImageLocationFromSequenceDescription;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.imageFormatToFolderName;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.isImageValid;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.isSpimData2D;

public class ImagesCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    ProjectCreator projectCreator;

    /**
     * Make an imagesCreator - includes all functions for adding images to a project
     * @param projectCreator projectCreator
     */
    public ImagesCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private String getDefaultLocalImagePath( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        if ( imageDataFormat == ImageDataFormat.OmeZarr ) {
            return getDefaultLocalImageZarrPath( datasetName, imageName, imageDataFormat );
        } else {
            return getDefaultLocalImageXmlPath( datasetName, imageName, imageDataFormat );
        }
    }

    private String getDefaultLocalImageXmlPath( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        return IOHelper.combinePath(getDefaultLocalImageDirPath(datasetName, imageDataFormat),
                imageName + ".xml");
    }

    private String getDefaultLocalImageZarrPath( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        return IOHelper.combinePath(getDefaultLocalImageDirPath(datasetName, imageDataFormat),
                imageName + ".ome.zarr");
    }

    private String getDefaultLocalN5Path( String datasetName, String imageName ) {
        return IOHelper.combinePath( getDefaultLocalImageDirPath( datasetName, ImageDataFormat.BdvN5),
                imageName + ".n5" );
    }

    private String getDefaultLocalImageDirPath( String datasetName, ImageDataFormat imageDataFormat ) {
        return IOHelper.combinePath(projectCreator.getProjectLocation().getAbsolutePath(), datasetName,
                "images", imageFormatToFolderName( imageDataFormat ) );
    }

    private String getDefaultTableDirPath( String datasetName, String imageName ) {
        return IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "tables", imageName );
    }

    private String getDefaultTablePath( String datasetName, String imageName ) {
        return IOHelper.combinePath( getDefaultTableDirPath(datasetName, imageName), "default.tsv" );
    }

    public boolean imageExists( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        // either xml file path or zarr file path depending on imageDataFormat
        String filePath = getDefaultLocalImagePath( datasetName, imageName, imageDataFormat );
        return new File (filePath).exists();
    }

    // with exclusive=false
    public void addImage ( ImagePlus imp, String imageName, String datasetName, ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType, AffineTransform3D sourceTransform, String uiSelectionGroup ) throws SpimDataException, IOException
    {
        addImage( imp, imageName, datasetName, imageDataFormat, imageType, sourceTransform, uiSelectionGroup, false, null, null, null );
    }

    /**
     *  Same as
     *  {@link #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D,
     *  String, boolean, int[][], int[][], Compression) }, but calculates reasonable defaults for resolutions,
     *  subdivisions and compression settings.
     *
     * @see #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D, String, boolean, int[][], int[][], Compression)
     */
    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           AffineTransform3D sourceTransform, String uiSelectionGroup,
                           boolean exclusive ) throws SpimDataException, IOException {
        addImage( imp, imageName, datasetName, imageDataFormat, imageType, sourceTransform, uiSelectionGroup,
                exclusive, null, null, null );

    }

    /**
     *  Same as
     *  {@link #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D,
     *  String, boolean, int[][], int[][], Compression) }, but assumes identity sourceTransform, and calculates
     *  reasonable defaults for resolutions, subdivisions and compression settings.
     *
     * @see #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D, String, boolean, int[][], int[][], Compression)
     */
    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           String uiSelectionGroup, boolean exclusive ) throws SpimDataException, IOException {
        addImage( imp, imageName, datasetName, imageDataFormat, imageType, new AffineTransform3D(), uiSelectionGroup,
                exclusive, null, null, null );

    }

    /**
     *  Same as
     *  {@link #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D,
     *  String, boolean, int[][], int[][], Compression) }, but assumes identity sourceTransform
     *
     * @see #addImage(ImagePlus, String, String, ImageDataFormat, ProjectCreator.ImageType, AffineTransform3D, String, boolean, int[][], int[][], Compression)
     */
    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           String uiSelectionGroup, boolean exclusive,
                           int[][] resolutions, int[][] subdivisions, Compression compression ) throws SpimDataException, IOException {
        addImage( imp, imageName, datasetName, imageDataFormat, imageType, new AffineTransform3D(), uiSelectionGroup,
                exclusive, resolutions, subdivisions, compression );
    }

    /**
     * Add an image to a MoBIE project. Make sure the ImagePlus scale and unit is set properly, so that imp.getCalibration()
     * returns the correct values. Note that multi-channel images are not supported - you will need to
     * split these channels and add each as its own image.
     * @param imp image to add
     * @param imageName image name
     * @param datasetName dataset name
     * @param imageDataFormat image format
     * @param imageType Image or Segmentation - segmentations will additionally generate a table.
     * @param sourceTransform Affine transform - this will be added to the image view, and will be added on top
     *                        of the normal scaling coming from imp.getCalibration()
     * @param uiSelectionGroup Name of MoBIE drop-down menu to place view in
     * @param exclusive Whether to make the view exclusive.
     * @param resolutions Resolution/downsampling levels to write e.g. new int[][]{ {1,1,1}, {2,2,2}, {4,4,4} }
     *                    will write one full resolution level, then one 2x downsampled, and one 4x downsampled.
     *                    The order is {x, y, z}.
     * @param subdivisions Chunk size. Must have the same number of entries as 'resolutions'. e.g.
     *                     new int[][]{ {64,64,64}, {64,64,64}, {64,64,64} }. The order is {x, y, z}.
     * @param compression type of compression to use
     * @throws SpimDataException
     * @throws IOException
     */
    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           AffineTransform3D sourceTransform, String uiSelectionGroup, boolean exclusive,
                           int[][] resolutions, int[][] subdivisions, Compression compression ) throws IOException, SpimDataException {
        // either xml file path or zarr file path depending on imageDataFormat
        String filePath = getDefaultLocalImagePath( datasetName, imageName, imageDataFormat );
        File imageFile = new File(filePath);

        if ( !isImageValid( imp.getNChannels(), imp.getCalibration().getUnit(),
                projectCreator.getVoxelUnit(), false ) ) {
            return;
        }

        if ( imp.getNDimensions() > 2 && projectCreator.getDataset( datasetName ).is2D ) {
            throw new UnsupportedOperationException("Can't add a " + imp.getNDimensions() + "D image to a 2D dataset" );
        }

        if ( projectCreator.getVoxelUnit() == null ) {
            projectCreator.setVoxelUnit( imp.getCalibration().getUnit() );
        }

        if ( imageFile.exists() ) {
            IJ.log("Overwriting image " + imageName + " in dataset " + datasetName );
            deleteImageFiles( datasetName, imageName, imageDataFormat );
        }

        DownsampleBlock.DownsamplingMethod downsamplingMethod = getDownsamplingMethod( imageType );

        File imageDir = new File(imageFile.getParent());
        if ( !imageDir.exists() ) {
            imageDir.mkdirs();
        }

        if ( resolutions == null || subdivisions == null || compression == null ) {
            writeDefaultImage( imp, filePath, downsamplingMethod, imageName, imageDataFormat );
        } else {
            writeDefaultImage( imp, filePath, downsamplingMethod, imageName, imageDataFormat,
                    resolutions, subdivisions, compression );
        }

        // check image written successfully, before writing jsons
        if ( imageFile.exists() ) {
            if (imageType == ProjectCreator.ImageType.image) {
                double[] contrastLimits = new double[]{imp.getDisplayRangeMin(), imp.getDisplayRangeMax()};
                LUT lut = imp.getLuts()[0];
                // FIXME: use ColorHelper.toString( lut );
                String colour = "r=" + lut.getRed(255) + ",g=" + lut.getGreen(255) + ",b=" +
                        lut.getBlue(255) + ",a=" + lut.getAlpha(255);
                updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup,
                        imageDataFormat, contrastLimits, colour, exclusive, sourceTransform );
            } else {
                updateTableAndJsonsForNewSegmentation(imageName, datasetName, uiSelectionGroup, imageDataFormat, exclusive, sourceTransform );
            }
        }

    }

    private DownsampleBlock.DownsamplingMethod getDownsamplingMethod( ProjectCreator.ImageType imageType ) {
        DownsampleBlock.DownsamplingMethod downsamplingMethod;
        switch( imageType ) {
            case image:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Average;
                break;
            default:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Centre;
        }

        return downsamplingMethod;
    }

    private void writeDefaultImage( ImagePlus imp, String filePath,
                                    DownsampleBlock.DownsamplingMethod downsamplingMethod,
                                    String imageName, ImageDataFormat imageDataFormat ) {

        // transform only including scaling from image
        AffineTransform3D sourceTransform = ProjectCreatorHelper.generateDefaultAffine( imp );

        // gzip compression by default
        switch( imageDataFormat ) {
            case BdvN5:
                new WriteImagePlusToN5().export(imp, filePath, sourceTransform, downsamplingMethod,
                        new GzipCompression(), new String[]{imageName} );
                break;

            case OmeZarr:
                new WriteImagePlusToN5OmeZarr().export(imp, filePath, sourceTransform, downsamplingMethod,
                        new GzipCompression() );
                break;

            default:
                throw new UnsupportedOperationException();

        }
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

    private void deleteImageFiles( String datasetName, String imageName, ImageDataFormat imageDataFormat ) throws IOException {

        File xmlFile = null;
        File n5File = null;
        File zarrFile = null;
        switch( imageDataFormat ) {
            case BdvN5:
                xmlFile = new File( getDefaultLocalImageXmlPath( datasetName, imageName, imageDataFormat ) );
                n5File = new File( getDefaultLocalN5Path( datasetName, imageName ) );
                break;

            case OmeZarr:
                zarrFile = new File( getDefaultLocalImageZarrPath( datasetName, imageName, imageDataFormat ) );
                break;

            default:
                throw new UnsupportedOperationException();

        }

        File tableFile = new File( getDefaultTablePath( datasetName, imageName ) );

        // delete files
        for ( File file : new File[] {xmlFile, tableFile}) {
            if ( file != null && file.exists() ) {
                Files.delete( file.toPath() );
            }
        }

        // delete directories
        for ( File file : new File[]{ zarrFile, n5File }) {
            if ( file != null && file.exists() ) {
                FileUtils.deleteDirectory( file );
            }
        }
    }

    /**
     * Add a bdv format image to a MoBIE project (e.g. N5 or OME-ZARR). Make sure the scale and unit is set properly
     * in the file. Note that multi-channel images are not supported - you will need to split these channels and add
     * each as its own image.
     * @param fileLocation Location of image file to add - for n5, location of the xml,
     *                     for ome-zarr, the location of the .ome.zarr directory.
     * @param imageName image name
     * @param datasetName dataset name
     * @param imageType Image or Segmentation - segmentations will additionally generate a table.
     * @param addMethod Add method - link (leave image as-is, and link to this location. Only supported for
     *                  N5 and local projects), copy (copy image into project), or move (move image into project -
     *                  be careful as this will delete the image from its original location!)
     * @param uiSelectionGroup Name of MoBIE drop-down menu to place view in
     * @param imageDataFormat image format
     * @param exclusive Whether to make the view exclusive.
     * @throws SpimDataException
     * @throws IOException
     */
    public void addBdvFormatImage ( File fileLocation, String imageName, String datasetName,
									ProjectCreator.ImageType imageType, ProjectCreator.AddMethod addMethod,
									String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive ) throws SpimDataException, IOException {

        if ( fileLocation.exists() ) {
            SpimData spimData = ( SpimData ) new SpimDataOpener().openSpimData( fileLocation.getAbsolutePath(), imageDataFormat );
            addBdvFormatImage( spimData, imageName, datasetName, imageType, addMethod, uiSelectionGroup,
                    imageDataFormat, exclusive );
        } else {
            throw new FileNotFoundException(
                    "Adding image to project failed - " + fileLocation.getAbsolutePath() + " does not exist" );
        }
    }

    /**
     * Add a bdv format image to a MoBIE project (e.g. N5 or OME-ZARR). Make sure the scale and unit is set properly
     * in the file. Note that multi-channel images are not supported - you will need to split these channels and add
     * each as its own image.
     * @param spimData spimData of n5 or ome-zarr file
     * @param imageName image name
     * @param datasetName dataset name
     * @param imageType Image or Segmentation - segmentations will additionally generate a table.
     * @param addMethod Add method - link (leave image as-is, and link to this location. Only supported for
     *                  N5 and local projects), copy (copy image into project), or move (move image into project -
     *                  be careful as this will delete the image from its original location!)
     * @param uiSelectionGroup Name of MoBIE drop-down menu to place view in
     * @param imageDataFormat image format
     * @param exclusive Whether to make the view exclusive.
     * @throws SpimDataException
     * @throws IOException
     */
    public void addBdvFormatImage ( SpimData spimData, String imageName, String datasetName,
									ProjectCreator.ImageType imageType, ProjectCreator.AddMethod addMethod,
									String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive ) throws SpimDataException, IOException {

        File imageDirectory = new File( getDefaultLocalImageDirPath( datasetName, imageDataFormat ));

        int nChannels = spimData.getSequenceDescription().getViewSetupsOrdered().size();
        String imageUnit = spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();

        if ( !isImageValid( nChannels, imageUnit, projectCreator.getVoxelUnit(), true ) ) {
            return;
        }

        if ( !isSpimData2D(spimData) && projectCreator.getDataset( datasetName ).is2D ) {
            throw new UnsupportedOperationException("Can't add a 3D image to a 2D dataset" );
        }

        if ( projectCreator.getVoxelUnit() == null ) {
            projectCreator.setVoxelUnit( imageUnit );
        }

        File newImageFile = null;
        switch( imageDataFormat ) {
            case BdvN5:
                newImageFile = new File(imageDirectory, imageName + ".xml");
                // The view setup name must be the same as the image name
                spimData = fixSetupName( spimData, imageName );
                break;

            case OmeZarr:
                newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
                break;
        }

        if ( newImageFile.exists() ) {
            IJ.log("Overwriting image " + imageName + " in dataset " + datasetName );
            deleteImageFiles( datasetName, imageName, imageDataFormat );
        }

        // make directory for that image file format, if doesn't exist already
        File imageDir = new File( newImageFile.getParent() );
        if ( !imageDir.exists() ) {
            imageDir.mkdirs();
        }

        switch (addMethod) {
            case link:
                // TODO - linking currently not supported for ome-zarr
                spimData.setBasePath( imageDir );
                new XmlIoSpimData().save(spimData, newImageFile.getAbsolutePath());
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

        IJ.log( "Bdv format image " + imageName + " added to project" );
    }

    private ArrayList<Object[]> makeDefaultTableRowsForTimepoint( Source labelsSource, int timepoint, boolean addTimepointColumn ) {

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
            String filePath = getDefaultLocalImagePath( datasetName, imageName, imageDataFormat );
            SpimData spimData = tryOpenSpimData( imageDataFormat, filePath );
            final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );
            final SourceAndConverter<?> sourceAndConverter = creator.getSetupIdToSourceAndConverter().values().iterator().next();
            final Source labelsSource = sourceAndConverter.getSpimSource();

            boolean hasTimeColumn = spimData.getSequenceDescription().getTimePoints().size() > 1;
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
            if ( hasTimeColumn ) {
                columnNames.add("timepoint");
            }

            ArrayList<Object[]> rows = new ArrayList<>();

            for ( Integer timepoint: spimData.getSequenceDescription().getTimePoints().getTimePoints().keySet() ) {
                rows.addAll( makeDefaultTableRowsForTimepoint( labelsSource, timepoint, hasTimeColumn ) );
            }

            Object[][] rowArray = new Object[rows.size()][columnNames.size()];
            rowArray = rows.toArray(rowArray);

            JTable table = new JTable(rowArray, columnNames.toArray() );
            Tables.saveTable( table, defaultTable );

            IJ.log( "Default table complete" );
        }
    }

    private SpimData tryOpenSpimData( ImageDataFormat imageDataFormat, String filePath )
    {
        try
        {
            return ( SpimData ) new SpimDataOpener().openSpimData( filePath, imageDataFormat );
        } catch ( SpimDataException e )
        {
           throw new RuntimeException( e );
        }
    }

    private void updateTableAndJsonsForNewImage ( String imageName, String datasetName, String uiSelectionGroup, ImageDataFormat imageDataFormat, double[] contrastLimits, String colour, boolean exclusive, AffineTransform3D sourceTransform ) {
        DatasetJsonCreator datasetJsonCreator = projectCreator.getDatasetJsonCreator();
        datasetJsonCreator.addImage( imageName, datasetName, uiSelectionGroup,
                imageDataFormat, contrastLimits, colour, exclusive, sourceTransform );
    }

    private void updateTableAndJsonsForNewSegmentation( String imageName, String datasetName, String uiSelectionGroup, ImageDataFormat imageDataFormat, boolean exclusive, AffineTransform3D sourceTransform ) {
        addDefaultTableForImage( imageName, datasetName, imageDataFormat );
        DatasetJsonCreator datasetJsonCreator = projectCreator.getDatasetJsonCreator();
        datasetJsonCreator.addSegmentation( imageName, datasetName, uiSelectionGroup, imageDataFormat, exclusive, sourceTransform );
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

    private void closeImgLoader ( SpimData spimData, ImageDataFormat imageFormat ) {
        BasicImgLoader imgLoader = spimData.getSequenceDescription().getImgLoader();

        switch ( imageFormat ) {
            case BdvN5:
                if ( imgLoader instanceof  N5ImageLoader ) {
                    ( (N5ImageLoader) imgLoader ).close();
                } else if ( imgLoader instanceof N5FSImageLoader ) {
                    ( (N5FSImageLoader) imgLoader ).close();
                }
                break;

            case OmeZarr:
                if (imgLoader instanceof N5OMEZarrImageLoader ) {
                    ( (N5OMEZarrImageLoader) imgLoader ).close();
                }
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

    private void writeNewBdvXml ( SpimData spimData, File imageFile, File saveDirectory, String imageName,
                                  ImageDataFormat imageFormat ) throws SpimDataException {

        ImgLoader imgLoader = null;
        if (imageFormat == ImageDataFormat.BdvN5) {
            imgLoader = new N5ImageLoader(imageFile, null);
        }

        spimData.setBasePath( saveDirectory );
        spimData.getSequenceDescription().setImgLoader(imgLoader);
        new XmlIoSpimData().save(spimData, new File( saveDirectory, imageName + ".xml").getAbsolutePath() );
    }

}
