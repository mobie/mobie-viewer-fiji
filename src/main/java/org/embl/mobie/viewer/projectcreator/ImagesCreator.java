package org.embl.mobie.viewer.projectcreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.process.LUT;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.n5.loaders.N5FSImageLoader;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.n5.writers.WriteImgPlusToN5;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.readers.N5OmeZarrReader;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5BdvOmeZarr;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5OmeZarr;

import org.embl.mobie.viewer.projectcreator.ui.ManualExportPanel;
import org.embl.mobie.io.util.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import mpicbg.spim.data.sequence.SequenceDescription;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper.*;
import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;
import static net.imglib2.util.Util.getTypeFromInterval;

public class ImagesCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    ProjectCreator projectCreator;

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
        return FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(), datasetName,
                "images", imageFormatToFolderName( imageDataFormat ), imageName + ".xml");
    }

    private String getDefaultLocalImageZarrPath( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        return FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(), datasetName,
                "images", imageFormatToFolderName( imageDataFormat ), imageName + ".ome.zarr");
    }

    private String getDefaultLocalImageDirPath( String datasetName, ImageDataFormat imageDataFormat ) {
        return FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(), datasetName,
                "images", imageFormatToFolderName( imageDataFormat ) );
    }

    private String getDefaultTableDirPath( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(), datasetName, "tables", imageName );
    }

    public boolean imageExists( String datasetName, String imageName, ImageDataFormat imageDataFormat ) {
        // either xml file path or zarr file path depending on imageDataFormat
        String filePath = getDefaultLocalImagePath( datasetName, imageName, imageDataFormat );
        return new File (filePath).exists();
    }

    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           AffineTransform3D sourceTransform, String uiSelectionGroup,
                           boolean exclusive ) {
        addImage( imp, imageName, datasetName, imageDataFormat, imageType, sourceTransform, uiSelectionGroup,
                exclusive, null, null, null );

    }

    public void addImage ( ImagePlus imp, String imageName, String datasetName,
                           ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                           AffineTransform3D sourceTransform, String uiSelectionGroup, boolean exclusive,
                           int[][] resolutions, int[][] subdivisions, Compression compression ) {
        // either xml file path or zarr file path depending on imageDataFormat
        String filePath = getDefaultLocalImagePath( datasetName, imageName, imageDataFormat );
        File imageFile = new File(filePath);

        if ( imageFile.exists() ) {
            IJ.log("Overwriting image " + imageName + " in dataset " + datasetName );
        }

        DownsampleBlock.DownsamplingMethod downsamplingMethod = getDownsamplingMethod( imageType );

        File imageDir = new File(imageFile.getParent());
        if ( !imageDir.exists() ) {
            imageDir.mkdirs();
        }

        if ( resolutions == null || subdivisions == null || compression == null ) {
            writeDefaultImage( imp, filePath, sourceTransform, downsamplingMethod, imageName, imageDataFormat );
        } else {
            writeDefaultImage( imp, filePath, sourceTransform, downsamplingMethod, imageName, imageDataFormat,
                    resolutions, subdivisions, compression );
        }

        // check image written successfully, before writing jsons
        if ( imageFile.exists() ) {
            boolean is2D;
            if ( imp.getNDimensions() <= 2 ) {
                is2D = true;
            } else {
                is2D = false;
            }
            try {
                if (imageType == ProjectCreator.ImageType.image) {
                    double[] contrastLimits = new double[]{imp.getDisplayRangeMin(), imp.getDisplayRangeMax()};
                    LUT lut = imp.getLuts()[0];
                    String colour = "r=" + lut.getRed(255) + ",g=" + lut.getGreen(255) + ",b=" +
                            lut.getBlue(255) + ",a=" + lut.getAlpha(255);
                    updateTableAndJsonsForNewImage(imageName, datasetName, uiSelectionGroup, is2D,
                            imp.getNFrames(), imageDataFormat, contrastLimits, colour, exclusive );
                } else {
                    updateTableAndJsonsForNewSegmentation(imageName, datasetName, uiSelectionGroup, is2D,
                            imp.getNFrames(), imageDataFormat, exclusive );
                }
            } catch (SpimDataException e) {
                e.printStackTrace();
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

    private void writeDefaultImage( ImagePlus imp, String filePath, AffineTransform3D sourceTransform,
                                    DownsampleBlock.DownsamplingMethod downsamplingMethod,
                                    String imageName, ImageDataFormat imageDataFormat ) {

        // gzip compression by default
        switch( imageDataFormat ) {
            case BdvN5:
                new WriteImgPlusToN5().export(imp, filePath, sourceTransform, downsamplingMethod,
                        new GzipCompression(), new String[]{imageName} );
                break;

            case BdvOmeZarr:
                new WriteImgPlusToN5BdvOmeZarr().export(imp, filePath, sourceTransform, downsamplingMethod,
                        new GzipCompression(), new String[]{imageName} );
                break;

            case OmeZarr:
                new WriteImgPlusToN5OmeZarr().export(imp, filePath, sourceTransform, downsamplingMethod,
                        new GzipCompression(), new String[]{imageName});
                break;

            default:
                throw new UnsupportedOperationException();

        }
    }

    private void writeDefaultImage( ImagePlus imp, String filePath, AffineTransform3D sourceTransform,
                             DownsampleBlock.DownsamplingMethod downsamplingMethod,
                             String imageName, ImageDataFormat imageDataFormat,
                             int[][] resolutions, int[][] subdivisions, Compression compression ) {

        switch( imageDataFormat ) {
            case BdvN5:
                new WriteImgPlusToN5().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            case BdvOmeZarr:
                new WriteImgPlusToN5BdvOmeZarr().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            case OmeZarr:
                new WriteImgPlusToN5OmeZarr().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            default:
                throw new UnsupportedOperationException();
        }
    }

    public void addBdvFormatImage ( File fileLocation, String datasetName, ProjectCreator.ImageType imageType,
                                    ProjectCreator.AddMethod addMethod, String uiSelectionGroup,
                                    ImageDataFormat imageDataFormat, boolean exclusive ) throws SpimDataException, IOException {

        if ( fileLocation.exists() ) {

            SpimData spimData = ( SpimData ) new SpimDataOpener().openSpimData( fileLocation.getAbsolutePath(), imageDataFormat );
            String imageName = fileLocation.getName().split("\\.")[0];
            File imageDirectory = new File( getDefaultLocalImageDirPath( datasetName, imageDataFormat ));

            File newImageFile = null;
            switch( imageDataFormat ) {
                case BdvN5:

                case BdvOmeZarr:
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
            }

            // make directory for that image file format, if doesn't exist already
            File imageDir = new File( newImageFile.getParent() );
            if ( !imageDir.exists() ) {
                imageDir.mkdirs();
            }

            switch (addMethod) {
                case link:
                    // TODO - linking currently not supported for ome-zarr
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
                updateTableAndJsonsForNewImage( imageName, datasetName, uiSelectionGroup,
                        isSpimData2D(spimData), getNTimepointsFromSpimData(spimData),
                        imageDataFormat, new double[]{0.0, 255.0}, "white", exclusive );
            } else {
                updateTableAndJsonsForNewSegmentation( imageName, datasetName, uiSelectionGroup,
                        isSpimData2D(spimData), getNTimepointsFromSpimData(spimData), imageDataFormat, exclusive );
            }

            IJ.log( "Bdv format image " + imageName + " added to project" );
        } else {
            IJ.log( "Adding image to project failed - " + fileLocation.getAbsolutePath() + " does not exist" );
        }
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
        File defaultTable = new File( tableFolder, "default.tsv" );
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

    private void updateTableAndJsonsForNewImage ( String imageName, String datasetName, String uiSelectionGroup,
                                                  boolean is2D, int nTimepoints, ImageDataFormat imageDataFormat,
                                                  double[] contrastLimits, String colour,
                                                  boolean exclusive ) throws SpimDataException {
        DatasetJsonCreator datasetJsonCreator = projectCreator.getDatasetJsonCreator();
        datasetJsonCreator.addImageToDatasetJson( imageName, datasetName, uiSelectionGroup, is2D, nTimepoints,
                imageDataFormat, contrastLimits, colour, exclusive );
    }

    private void updateTableAndJsonsForNewSegmentation( String imageName, String datasetName, String uiSelectionGroup,
                                                        boolean is2D, int nTimepoints, ImageDataFormat imageDataFormat,
                                                        boolean exclusive ) {
        addDefaultTableForImage( imageName, datasetName, imageDataFormat );
        DatasetJsonCreator datasetJsonCreator = projectCreator.getDatasetJsonCreator();
        datasetJsonCreator.addSegmentationToDatasetJson( imageName, datasetName, uiSelectionGroup, is2D, nTimepoints,
                imageDataFormat, exclusive );
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

            case BdvOmeZarr:
                newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
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

            case BdvOmeZarr:
                newImageFile = new File(imageDirectory, imageName + ".ome.zarr" );
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

            case BdvOmeZarr:

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
                                  ImageDataFormat imageFormat ) throws SpimDataException, IOException {

        ImgLoader imgLoader = null;
        switch ( imageFormat ) {
            case BdvN5:
                imgLoader = new N5ImageLoader( imageFile, null);
                break;

            case BdvOmeZarr:
                imgLoader = new N5OMEZarrImageLoader(
                        new N5OmeZarrReader(imageFile.getAbsolutePath()), spimData.getSequenceDescription());
                break;

        }

        spimData.setBasePath( saveDirectory );
        spimData.getSequenceDescription().setImgLoader(imgLoader);
        new XmlIoSpimData().save(spimData, new File( saveDirectory, imageName + ".xml").getAbsolutePath() );
    }

    private void addAffineTransformToXml ( String xmlPath, String affineTransform )  {
        if ( affineTransform != null ) {
            try {
                SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( xmlPath );
                int numTimepoints = spimDataMinimal.getSequenceDescription().getTimePoints().size();
                int numSetups = spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().size();

                AffineTransform3D sourceTransform = new AffineTransform3D();
                String[] splitAffineTransform = affineTransform.split(" ");
                double[] doubleAffineTransform = new double[splitAffineTransform.length];
                for ( int i = 0; i < splitAffineTransform.length; i++ ) {
                    doubleAffineTransform[i] = Double.parseDouble( splitAffineTransform[i] );
                }
                sourceTransform.set( doubleAffineTransform );

                final ArrayList<ViewRegistration> registrations = new ArrayList<>();
                for ( int t = 0; t < numTimepoints; ++t ) {
                    for (int s = 0; s < numSetups; ++s) {
                        registrations.add(new ViewRegistration(t, s, sourceTransform));
                    }
                }

                SpimDataMinimal updatedSpimDataMinimial = new SpimDataMinimal(spimDataMinimal.getBasePath(),
                        spimDataMinimal.getSequenceDescription(), new ViewRegistrations( registrations) );

                new XmlIoSpimDataMinimal().save( updatedSpimDataMinimial, xmlPath);
            } catch (SpimDataException e) {
                IJ.log( "Error adding affine transform to xml file. Check xml manually.");
                e.printStackTrace();
            }

        }
    }

}
