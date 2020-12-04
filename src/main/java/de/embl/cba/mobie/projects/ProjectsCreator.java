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
import ij.IJ;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

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


            // if this is the first dataset, then make this the default
            if (currentDatasets.datasets.size() == 0) {
                currentDatasets.defaultDataset = name;
            }
            currentDatasets.datasets.add(name);
            try {
                writeDatasetsJson();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void makeDefaultDataset ( String datasetName ) {
        updateCurrentDatasets();

        currentDatasets.defaultDataset = datasetName;
        try {
            writeDatasetsJson();
        } catch (IOException e) {
            e.printStackTrace();
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

    public ImageProperties getImageProperties ( String datasetName, String imageName ) {
        updateCurrentImages( datasetName );
        return currentImages.get( imageName );
    }

    public String[] getCurrentImages( String datasetName ) {
        updateCurrentImages( datasetName );
        if ( currentImages.size() > 0 ) {
            Set<String> imageNames = currentImages.keySet();
            String[] imageNamesArray = new String[imageNames.size()];
            imageNames.toArray( imageNamesArray );
            return imageNamesArray;
        } else {
            return new String[] {""};
        }
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

    public boolean isInDatasets ( String datasetName ) {
        return Arrays.stream( getCurrentDatasets() ).anyMatch(datasetName::equals);
    }

    public boolean isDefaultDataset( String datasetName ) {
        return currentDatasets.defaultDataset.equals( datasetName );
    }

    public void renameDataset( String oldName, String newName ) {
        updateCurrentDatasets();

        File oldDatasetDir = new File ( dataLocation, oldName );
        File newDatasetDir = new File ( dataLocation, newName );

        if ( oldDatasetDir.exists() ) {
            if (oldDatasetDir.renameTo(newDatasetDir)) {
                // update json
                if ( currentDatasets.defaultDataset.equals(oldName) ) {
                    currentDatasets.defaultDataset = newName;
                }

                int indexOld = currentDatasets.datasets.indexOf( oldName );
                currentDatasets.datasets.set( indexOld, newName );

                try {
                    writeDatasetsJson();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                IJ.log( "Rename file failed");
            }
        }

    }

    public void updateCurrentImages( String datasetName ) {
        File imagesJSON = new File( getImagesJsonPath( datasetName ) );

        if ( imagesJSON.exists() ) {
            currentImages = new ImagesJsonParser( getDatasetPath( datasetName ) ).getImagePropertiesMap();
        } else {
            currentImages = new HashMap<>();
        }
    }

    // TODO - is this efficient for big images?
    private void addDefaultTableForImage ( String imageName, String datasetName ) {
        File tableFolder = new File( FileAndUrlUtils.combinePath( getDatasetPath( datasetName ), "tables", imageName));
        File defaultTable = new File( tableFolder, "default.csv");
        if ( !tableFolder.exists() ){
            tableFolder.mkdirs();
        }

        if ( !defaultTable.exists() ) {

            String[] columnNames = {"label_id", "anchor_x", "anchor_y",
                    "anchor_z", "bb_min_x", "bb_min_y", "bb_min_z", "bb_max_x",
                    "bb_max_y", "bb_max_z"};

            final LazySpimSource labelsSource = new LazySpimSource("labelImage", getLocalImageXmlPath(datasetName, imageName));

            // has to already be as a labeling type
            // TODO - warn needs to be integer, 0 counted as background
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
        }
    }

    public void addToImagesJson ( String imageName, String imageType, String datasetName ) {
        updateCurrentImages( datasetName );
        ImageProperties newImageProperties = new ImageProperties();
        newImageProperties.type = imageType;
        if ( imageType.equals("segmentation") ) {
            newImageProperties.color = ColoringLuts.GLASBEY;
            newImageProperties.tableFolder = "tables/" + imageName;
            addDefaultTableForImage( imageName, datasetName );
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
        try {
            new ImagesJsonParser( getDatasetPath( datasetName) ).writeImagePropertiesMap( getImagesJsonPath( datasetName),
                    currentImages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeDatasetsJson () throws IOException {
        File datasetJSON = new File(dataLocation, "datasets.json");
        new DatasetsParser().datasetsToFile(datasetJSON.getAbsolutePath(), currentDatasets);
    }
}
