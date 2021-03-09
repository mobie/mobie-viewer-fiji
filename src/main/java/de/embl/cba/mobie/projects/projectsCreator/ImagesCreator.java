package de.embl.cba.mobie.projects.projectsCreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.n5.DownsampleBlock;
import de.embl.cba.mobie.n5.WriteImgPlusToN5;
import de.embl.cba.mobie.projects.projectsCreator.ui.ManualN5ExportPanel;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.GzipCompression;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;

public class ImagesCreator {

    ProjectsCreator projectsCreator;
    String datasetName;
    ImagesJsonCreator imagesJsonCreator;

    public ImagesCreator( ProjectsCreator projectsCreator, String datasetName ) {
        this.projectsCreator = projectsCreator;
        this.datasetName = datasetName;
        this.imagesJsonCreator = new ImagesJsonCreator();
    }

    public String[] getCurrentImageNames() {
        Map<String, ImageProperties> currentImagesProperties = imagesJsonCreator.getCurrentImageProperties();
        if ( currentImagesProperties.size() > 0 ) {
            Set<String> imageNames = currentImagesProperties.keySet();
            String[] imageNamesArray = new String[imageNames.size()];
            imageNames.toArray( imageNamesArray );
            return imageNamesArray;
        } else {
            return new String[] {""};
        }
    }

    public void addImage ( String imageName, ProjectsCreator.BdvFormat bdvFormat, ProjectsCreator.ImageType imageType,
                          AffineTransform3D sourceTransform, boolean useDefaultSettings ) {
        ImagePlus imp = IJ.getImage();
        String xmlPath = projectsCreator.getLocalImageXmlPath( datasetName, imageName);
        File xmlFile = new File( xmlPath );

        DownsampleBlock.DownsamplingMethod downsamplingMethod;
        switch( imageType ) {
            case image:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Average;
                break;
            default:
                downsamplingMethod = DownsampleBlock.DownsamplingMethod.Mode;
        }

        if ( !xmlFile.exists() ) {
            switch( bdvFormat ) {
                case n5:
                    if (!useDefaultSettings) {
                        new ManualN5ExportPanel(imp, xmlPath, sourceTransform, downsamplingMethod).getManualExportParameters();
                    } else {
                        // gzip compression by default
                        new WriteImgPlusToN5().export(imp, xmlPath, sourceTransform, downsamplingMethod,
                                new GzipCompression());
                    }
            }

            // check image written successfully, before writing jsons
            if ( xmlFile.exists() ) {
                updateJsonsForNewImage( imageName, imageType, datasetName );
            }

        } else {
            Utils.log( "Adding image to project failed - this image name already exists" );
        }
    }

    public void addBdvFormatImage (File xmlLocation, ProjectsCreator.ImageType imageType,
                                   ProjectsCreator.AddMethod addMethod ) throws SpimDataException, IOException {
        if ( xmlLocation.exists() ) {
            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load(xmlLocation.getAbsolutePath());
            String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
            File newXmlDirectory = new File(FileAndUrlUtils.combinePath(projectsCreator.getImagesDirectoryPath( datasetName ), "local"));
            File newXmlFile = new File(newXmlDirectory, imageName + ".xml");

            if ( !newXmlFile.exists() ) {
                // check n5 format (e.g. we no longer support hdf5)
                ProjectsCreator.BdvFormat bdvFormat = getBdvFormatFromSpimDataMinimal( spimDataMinimal );
                if ( bdvFormat != null ) {
                    switch (addMethod) {
                        case link:
                            new XmlIoSpimDataMinimal().save(spimDataMinimal, newXmlFile.getAbsolutePath());
                            break;
                        case copy:
                            copyImage(bdvFormat, spimDataMinimal, newXmlDirectory, imageName);
                            break;
                        case move:
                            moveImage(bdvFormat, spimDataMinimal, newXmlDirectory, imageName);
                            break;
                    }
                    updateJsonsForNewImage(imageName, imageType, datasetName);
                } else {
                    Utils.log( "Image is of unsupported type. Must be n5.");
                }
            } else {
                Utils.log("Adding image to project failed - this image name already exists");
            }
        } else {
            Utils.log( "Adding image to project failed - xml does not exist" );
        }
    }

    // TODO - is this efficient for big images?
    private void addDefaultTableForImage ( String imageName, String datasetName ) {
        File tableFolder = new File( projectsCreator.getTablesDirectoryPath( datasetName, imageName ));
        File defaultTable = new File( tableFolder, "default.csv");
        if ( !tableFolder.exists() ){
            tableFolder.mkdirs();
        }

        if ( !defaultTable.exists() ) {

            Utils.log( " Creating default table... 0 label is counted as background" );

            String[] columnNames = {"label_id", "anchor_x", "anchor_y",
                    "anchor_z", "bb_min_x", "bb_min_y", "bb_min_z", "bb_max_x",
                    "bb_max_y", "bb_max_z"};

            final LazySpimSource labelsSource = new LazySpimSource("labelImage",
                    projectsCreator.getLocalImageXmlPath(datasetName, imageName));

            final RandomAccessibleInterval<IntType> rai = labelsSource.getNonVolatileSource(0, 0);
            double[] dimensions = new double[ rai.numDimensions() ];
            labelsSource.getVoxelDimensions().dimensions( dimensions );

            ImgLabeling<Integer, IntType> imgLabeling = labelMapAsImgLabeling(rai);

            LabelRegions labelRegions = new LabelRegions(imgLabeling);
            Iterator<LabelRegion> labelRegionIterator = labelRegions.iterator();

            ArrayList<Object[]> rows = new ArrayList<>();
            while (labelRegionIterator.hasNext()) {
                Object[] row = new Object[columnNames.length];
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

                rows.add(row);
            }

            Object[][] rowArray = new Object[rows.size()][columnNames.length];
            rowArray = rows.toArray(rowArray);

            JTable table = new JTable(rowArray, columnNames);
            Tables.saveTable(table, new File(FileAndUrlUtils.combinePath(tableFolder.getAbsolutePath(), "default.csv")));

            Utils.log( "Default table complete" );
        }
    }

    private void copyImage (ProjectsCreator.BdvFormat bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocationFromSpimDataMinimal( spimDataMinimal, bdvFormat );

        switch ( bdvFormat ) {
            case n5:
                FileUtils.copyDirectory(imageLocation, newImageFile );
                break;
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat);
    }

    private void moveImage (ProjectsCreator.BdvFormat bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocationFromSpimDataMinimal( spimDataMinimal, bdvFormat );

        // have to explicitly close the image loader, so we can delete the original file
        closeImgLoader( spimDataMinimal, bdvFormat );

        switch ( bdvFormat ) {
            case n5:
                FileUtils.moveDirectory( imageLocation, newImageFile );
                break;
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat );
    }

    private void closeImgLoader ( SpimDataMinimal spimDataMinimal, ProjectsCreator.BdvFormat bdvFormat ) {
        BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();

        switch ( bdvFormat ) {
            case n5:
                N5ImageLoader n5ImageLoader = (N5ImageLoader) imgLoader;
                n5ImageLoader.close();
                break;
        }
    }

    private File getImageLocationFromSpimDataMinimal( SpimDataMinimal spimDataMinimal, ProjectsCreator.BdvFormat bdvFormat) {
        File imageLocation = null;

        switch ( bdvFormat ) {
            case n5:
                // get image loader to find absolute image location
                N5ImageLoader n5ImageLoader = (N5ImageLoader) spimDataMinimal.getSequenceDescription().getImgLoader();
                imageLocation = n5ImageLoader.getN5File();
                break;
        }

        return imageLocation;
    }

    private ProjectsCreator.BdvFormat getBdvFormatFromSpimDataMinimal(SpimDataMinimal spimDataMinimal ) {
        ProjectsCreator.BdvFormat bdvFormat = null;
        if ( spimDataMinimal.getSequenceDescription().getImgLoader() instanceof N5ImageLoader ) {
            bdvFormat = ProjectsCreator.BdvFormat.n5;
        }

        return bdvFormat;
    }

    private void writeNewBdvXml ( SpimDataMinimal spimDataMinimal, File imageFile, File saveDirectory, String imageName, ProjectsCreator.BdvFormat bdvFormat ) throws SpimDataException {

        ImgLoader imgLoader = null;
        switch ( bdvFormat ) {
            case n5:
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
                Utils.log( "Error adding affine transform to xml file. Check xml manually.");
                e.printStackTrace();
            }

        }
    }

    public boolean isInImages ( String imageName, String datasetName ) {
        return Arrays.stream( getCurrentImageNames( datasetName ) ).anyMatch(imageName::equals);
    }


}
