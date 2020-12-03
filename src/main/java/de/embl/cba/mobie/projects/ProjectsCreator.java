package de.embl.cba.mobie.projects;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagesJsonParser;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.image.Storage;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.color.ColoringLuts;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;

public class ProjectsCreator {
    private final File projectLocation;
    private final File dataLocation;
    private Datasets currentDatasets;
    private Map< String, ImageProperties> currentImages;

    public ProjectsCreator ( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
    }

    public void addImage ( String imagePath, String imageName, String datasetName, String bdvFormat,
                           String pixelSizeUnit, double xPixelSize, double yPixelSize, double zPixelSize) {
    //    https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java
    //    https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/export/n5/WriteSequenceToN5.java
    //    Need an image loader https://javadoc.scijava.org/Fiji/mpicbg/spim/data/generic/sequence/BasicImgLoader.html
    //    I'm curious if it might be easier to just open it in fiji as a virtual stack then run teh plugin....
    // or liek this https://syn.mrc-lmb.cam.ac.uk/acardona/fiji-tutorial/#imglib2-n5
    //    https://github.com/saalfeldlab/n5-imglib2
    //    n5 imglib2 looks pretty promising - get it to randomaccessible itnermval then write
    }


    public void addDataset ( String name ) {
        File datasetDir = new File ( dataLocation, name );
        updateCurrentDatasets();

        if ( !datasetDir.exists() ) {
            datasetDir.mkdirs();

            // make rest of folders required under dataset
            new File(datasetDir, "images").mkdirs();
            new File(datasetDir, "misc").mkdirs();
            new File(datasetDir, "tables").mkdirs();
            new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "images", "local")).mkdirs();
            new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "images", "remote")).mkdirs();
            new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "misc", "bookmarks")).mkdirs();

            File datasetJSON = new File(dataLocation, "datasets.json");

            // if this is the first dataset, then make this the default
            if (currentDatasets.datasets.size() == 0) {
                currentDatasets.defaultDataset = name;
            }
            currentDatasets.datasets.add(name);
            try {
                new DatasetsParser().datasetsToFile(datasetJSON.getAbsolutePath(), currentDatasets);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void updateCurrentDatasets() {
        File datasetJSON = new File( dataLocation, "datasets.json");

        if ( datasetJSON.exists() ) {
            currentDatasets = new DatasetsParser().fetchProjectDatasets(dataLocation.getAbsolutePath());
        } else {
            currentDatasets = new Datasets();
            currentDatasets.datasets = new ArrayList<>();
        }
    }

    public String[] getCurrentDatasets () {
        updateCurrentDatasets();
        if ( currentDatasets.datasets.size() > 0 ) {
            ArrayList<String> datasetNames = currentDatasets.datasets;
            String[] datasetNamesArray = new String[datasetNames.size()];
            datasetNames.toArray( datasetNamesArray );
            return datasetNamesArray;
        } else {
            return new String[] {""};
        }
    }

    public Map< String, ImageProperties> getCurrentImages( String datasetName ) {
        updateCurrentImages( datasetName );
        return currentImages;
    }

    public String getDatasetPath ( String datasetName ) {
        return FileAndUrlUtils.combinePath(dataLocation.getAbsolutePath(), datasetName);
    }

    private String getImagesJsonPath ( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName,
                "images", "images.json");
    }

        // TODO - is this handled by one of tischi's projectlocation classes already???
    private String getLocalImageXmlPath ( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath(dataLocation.getAbsolutePath(), datasetName, "images", "local", imageName + ".xml");
    }

    public String getImagesPath ( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName, "images");
    }

    public void updateCurrentImages( String datasetName ) {
        File imagesJSON = new File( getImagesJsonPath( datasetName ) );

        if ( imagesJSON.exists() ) {
            currentImages = new ImagesJsonParser( getDatasetPath( datasetName ) ).getImagePropertiesMap();
        } else {
            currentImages = new HashMap<>();
        }
    }

    private void addDefaultTableForImage ( String imageName, String datasetName ) {
        File tableFolder = new File( FileAndUrlUtils.combinePath( getDatasetPath( datasetName ), "tables", imageName));
        if ( !tableFolder.exists() ){
            tableFolder.mkdirs();
        }

        String[] columnNames = { "label_id", "anchor_x", "anchor_y",
                "anchor_z", "bb_min_x", "bb_min_y", "bb_min_z", "bb_max_x",
                "bb_max_y", "bb_max_z" };

        final LazySpimSource labelsSource = new LazySpimSource( "labelImage", getLocalImageXmlPath( datasetName, imageName) );
        // has to already be as a labeling type
        // warn needs to be integer, 0 counted as background
        final RandomAccessibleInterval<IntType> rai = labelsSource.getNonVolatileSource( 0, 0);
        ImgLabeling< Integer, IntType > imgLabeling = labelMapAsImgLabeling( rai );

        LabelRegions labelRegions = new LabelRegions( imgLabeling );
        Iterator<LabelRegion> labelRegionIterator = labelRegions.iterator();

        ArrayList<Object[]> rows = new ArrayList<>();
        // ArrayList<Integer> labelIds = new ArrayList<>();
        // ArrayList<double[]> centres = new ArrayList<>();
        // ArrayList<double[]> bbMins = new ArrayList<>();
        // ArrayList<double[]> bbMaxs = new ArrayList<>();
        while ( labelRegionIterator.hasNext() ) {
            Object[] row = new Object[columnNames.length ];
            LabelRegion labelRegion = labelRegionIterator.next();


            double[] centre = new double[rai.numDimensions()];
            labelRegion.getCenterOfMass().localize( centre );
            double[] bbMin = new double[rai.numDimensions()];
            double[] bbMax = new double[rai.numDimensions()];
            labelRegion.realMin( bbMin );
            labelRegion.realMax( bbMax );

            row[0] =  labelRegion.getLabel();
            row[1] = centre[0];
            row[2] = centre[1];
            row[3] = centre[2];
            row[4] = bbMin[0];
            row[5] = bbMin[1];
            row[6] = bbMin[2];
            row[7] = bbMax[0];
            row[8] = bbMax[1];
            row[9] = bbMax[2];

            rows.add( row );

        }

        Object[][] rowArray = new Object[ rows.size() ] [ columnNames.length ];
        rowArray = rows.toArray( rowArray );
        // compensate for spacing

        // make a Jtable
        JTable table = new JTable( rowArray, columnNames);
        Tables.saveTable( table, new File( FileAndUrlUtils.combinePath( tableFolder.getAbsolutePath(), "default.csv")) );
    }

    public void addToImagesJson ( String imageName, String imageType, String datasetName ) {
        updateCurrentImages( datasetName );
        ImageProperties newImageProperties = new ImageProperties();
        newImageProperties.type = imageType;
        if ( imageType.equals("segmentation") ) {
            newImageProperties.color = ColoringLuts.GLASBEY;
            newImageProperties.tableFolder = "tables/" + imageName;
            addDefaultTableForImage( imageName, datasetName );
            // TODO - make default.csv
        } else {
            newImageProperties.color = "white";
        }

        newImageProperties.contrastLimits = new double[] {0, 255};

        Storage storage = new Storage();
        storage.local = "local/" + imageName + ".xml";
        newImageProperties.storage = storage;

        currentImages.put( imageName, newImageProperties);

        writeImagesJson( datasetName );
    }

    public void writeImagesJson ( String datasetName ) {
        new ImagesJsonParser( getDatasetPath( datasetName) ).writeImagePropertiesMap( getImagesJsonPath( datasetName),
                currentImages);
    }


}
