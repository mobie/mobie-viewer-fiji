package de.embl.cba.mobie.projectcreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.n5.N5FSImageLoader;
import de.embl.cba.mobie.projectcreator.n5.DownsampleBlock;
import de.embl.cba.mobie.projectcreator.n5.WriteImgPlusToN5;
import de.embl.cba.mobie.projectcreator.ui.ManualN5ExportPanel;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
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
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.GzipCompression;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.*;
import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;
import static net.imglib2.util.Util.getTypeFromInterval;

public class ImagesCreator {

    ProjectCreator projectCreator;

    public ImagesCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private String getDefaultLocalImageXmlPath( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(), datasetName,
                "images", "local", imageName + ".xml");
    }

    private String getDefaultLocalImageDirPath( String datasetName ) {
        return FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(), datasetName,
                "images", "local" );
    }

    private String getDefaultTableDirPath( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(), datasetName, "tables", imageName );
    }

    public void addImage (ImagePlus imp, String imageName, String datasetName,
                          ImageDataFormat imageDataFormat, ProjectCreator.ImageType imageType,
                          AffineTransform3D sourceTransform, boolean useDefaultSettings, String uiSelectionGroup ) {
        String xmlPath = getDefaultLocalImageXmlPath( datasetName, imageName );
        File xmlFile = new File( xmlPath );

        DownsampleBlock.DownsamplingMethod downsamplingMethod;
        switch( imageType ) {
            case image:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Average;
                break;
            default:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Centre;
        }

        if ( !xmlFile.exists() ) {
            switch( imageDataFormat ) {
                case BdvN5:
                    if (!useDefaultSettings) {
                        new ManualN5ExportPanel(imp, xmlPath, sourceTransform, downsamplingMethod, imageName).getManualExportParameters();
                    } else {
                        // gzip compression by default
                        new WriteImgPlusToN5().export(imp, xmlPath, sourceTransform, downsamplingMethod,
                                new GzipCompression(), new String[]{imageName} );
                    }
            }

            // check image written successfully, before writing jsons
            if ( xmlFile.exists() ) {
                boolean is2D;
                if ( imp.getNDimensions() <= 2 ) {
                    is2D = true;
                } else {
                    is2D = false;
                }
                try {
                    updateTableAndJsonsForNewImage( imageName, imageType, datasetName, uiSelectionGroup, is2D, imp.getNFrames() );
                } catch (SpimDataException e) {
                    e.printStackTrace();
                }
            }
        } else {
            IJ.log( "Adding image to project failed - this image name already exists" );
        }
    }

    public void addBdvFormatImage ( File xmlLocation, String datasetName, ProjectCreator.ImageType imageType,
                                   ProjectCreator.AddMethod addMethod, String uiSelectionGroup ) throws SpimDataException, IOException {
        if ( xmlLocation.exists() ) {
            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load(xmlLocation.getAbsolutePath());
            String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
            File newXmlDirectory = new File( getDefaultLocalImageDirPath( datasetName ));
            File newXmlFile = new File(newXmlDirectory, imageName + ".xml");

            if ( !newXmlFile.exists() ) {
                // check n5 format (e.g. we no longer support hdf5)
                ImageDataFormat imageFormat = getImageFormatFromSpimDataMinimal( spimDataMinimal );
                if ( imageFormat != null && imageFormat.isSupportedByProjectCreator() ) {
                    // The view setup name must be the same as the image name
                    spimDataMinimal = fixSetupName( spimDataMinimal, imageName );

                    switch (addMethod) {
                        case link:
                            new XmlIoSpimDataMinimal().save(spimDataMinimal, newXmlFile.getAbsolutePath());
                            break;
                        case copy:
                            copyImage( imageFormat, spimDataMinimal, newXmlDirectory, imageName);
                            break;
                        case move:
                            moveImage( imageFormat, spimDataMinimal, newXmlDirectory, imageName);
                            break;
                    }
                    updateTableAndJsonsForNewImage( imageName, imageType, datasetName, uiSelectionGroup,
                            isSpimData2D( spimDataMinimal ), getNTimepointsFromSpimData( spimDataMinimal ) );
                } else {
                    IJ.log( "Image is of unsupported type.");
                }
            } else {
                IJ.log("Adding image to project failed - this image name already exists");
            }
        } else {
            IJ.log( "Adding image to project failed - xml does not exist" );
        }
    }

    private ArrayList<Object[]> makeDefaultTableRowsForTimepoint( LazySpimSource labelsSource, int timepoint, boolean addTimepointColumn ) {

        RandomAccessibleInterval rai = labelsSource.getNonVolatileSource( timepoint, 0 );
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
    private void addDefaultTableForImage ( String imageName, String datasetName ) throws SpimDataException {
        File tableFolder = new File( getDefaultTableDirPath( datasetName, imageName ) );
        File defaultTable = new File( tableFolder, "default.tsv" );
        if ( !tableFolder.exists() ){
            tableFolder.mkdirs();
        }

        if ( !defaultTable.exists() ) {

            IJ.log( " Creating default table... 0 label is counted as background" );

            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load(getDefaultLocalImageXmlPath(datasetName, imageName));

            boolean hasTimeColumn = spimDataMinimal.getSequenceDescription().getTimePoints().size() > 1;
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

            final LazySpimSource labelsSource = new LazySpimSource("labelImage",
                    getDefaultLocalImageXmlPath(datasetName, imageName));
            ArrayList<Object[]> rows = new ArrayList<>();

            for ( Integer timepoint: spimDataMinimal.getSequenceDescription().getTimePoints().getTimePoints().keySet() ) {
                rows.addAll( makeDefaultTableRowsForTimepoint( labelsSource, timepoint, hasTimeColumn ) );
            }

            Object[][] rowArray = new Object[rows.size()][columnNames.size()];
            rowArray = rows.toArray(rowArray);

            JTable table = new JTable(rowArray, columnNames.toArray() );
            Tables.saveTable( table, defaultTable );

            IJ.log( "Default table complete" );
        }
    }

    private void updateTableAndJsonsForNewImage ( String imageName, ProjectCreator.ImageType imageType,
                                          String datasetName, String uiSelectionGroup, boolean is2D, int nTimepoints ) throws SpimDataException {
        if ( imageType == ProjectCreator.ImageType.segmentation) {
            addDefaultTableForImage( imageName, datasetName );
        }
        DatasetJsonCreator datasetJsonCreator = projectCreator.getDatasetJsonCreator();
        datasetJsonCreator.addToDatasetJson( imageName, datasetName, imageType, uiSelectionGroup, is2D, nTimepoints );
    }

    private void copyImage ( ImageDataFormat imageFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = null;

        switch( imageFormat ) {
            case BdvN5:
                newImageFile = new File(newXmlDirectory, imageName + ".n5" );
                File imageLocation = getImageLocationFromSpimDataMinimal(spimDataMinimal, imageFormat );
                FileUtils.copyDirectory(imageLocation, newImageFile);
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, imageFormat );
    }

    private void moveImage ( ImageDataFormat imageFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = null;

        switch( imageFormat ) {
            case BdvN5:
                newImageFile = new File( newXmlDirectory, imageName + ".n5" );
                File imageLocation = getImageLocationFromSpimDataMinimal( spimDataMinimal, imageFormat );

                // have to explicitly close the image loader, so we can delete the original file
                closeImgLoader( spimDataMinimal, imageFormat );
                FileUtils.moveDirectory( imageLocation, newImageFile );
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, imageFormat );
    }

    private void closeImgLoader ( SpimDataMinimal spimDataMinimal, ImageDataFormat imageFormat ) {
        BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();

        switch ( imageFormat ) {
            case BdvN5:
                if ( imgLoader instanceof  N5ImageLoader ) {
                    ( (N5ImageLoader) imgLoader ).close();
                } else if ( imgLoader instanceof N5FSImageLoader ) {
                    ( (N5FSImageLoader) imgLoader ).close();
                }
                break;
        }
    }

    private SpimDataMinimal fixSetupName( SpimDataMinimal spimDataMinimal, String imageName ) {
        // The view setup name must be the same as the image name
        BasicViewSetup firstSetup = spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().get(0);
        if ( !firstSetup.getName().equals(imageName) ) {

            int numSetups = spimDataMinimal.getSequenceDescription().getViewSetups().size();
            final HashMap< Integer, BasicViewSetup> setups = new HashMap<>( numSetups );
            for ( int s = 0; s < numSetups; s++ )
            {
                final BasicViewSetup setup;
                if ( s == 0 ) {
                    setup = new BasicViewSetup( firstSetup.getId(), imageName,
                            firstSetup.getSize(), firstSetup.getVoxelSize() );
                    for ( Entity attribute: firstSetup.getAttributes().values() ) {
                        setup.setAttribute( attribute );
                    }
                } else {
                    setup = spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().get( s );
                }
                setups.put( s, setup );
            }

            final SequenceDescriptionMinimal newSeq = new SequenceDescriptionMinimal(
                    spimDataMinimal.getSequenceDescription().getTimePoints(), setups,
                    spimDataMinimal.getSequenceDescription().getImgLoader(), null );

            return new SpimDataMinimal( spimDataMinimal.getBasePath(), newSeq, spimDataMinimal.getViewRegistrations() );
        } else {
            return spimDataMinimal;
        }
    }

    private void writeNewBdvXml ( SpimDataMinimal spimDataMinimal, File imageFile, File saveDirectory, String imageName,
                                  ImageDataFormat imageFormat ) throws SpimDataException {

        ImgLoader imgLoader = null;
        switch ( imageFormat ) {
            case BdvN5:
                imgLoader = new N5ImageLoader( imageFile, null);
                break;
        }

        spimDataMinimal.setBasePath( saveDirectory );
        spimDataMinimal.getSequenceDescription().setImgLoader(imgLoader);
        new XmlIoSpimDataMinimal().save(spimDataMinimal, new File( saveDirectory, imageName + ".xml").getAbsolutePath() );
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
