package de.embl.cba.mobie.projects;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarksJsonParser;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.*;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.color.ColoringLuts;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;

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
    // holds all image properties for one chosen dataset
    private Map< String, ImageProperties> currentImagesProperties;
    private Map<String, Bookmark> currentDefaultBookmarks;

    public ProjectsCreator ( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
    }

    public void addImage ( String imageName, String datasetName, String bdvFormat, String imageType ) {
        // TODO - add to project creator
        String xmlPath = FileAndUrlUtils.combinePath(projectLocation.getAbsolutePath(), "data", datasetName, "images", "local", imageName + ".xml");
        if (bdvFormat.equals("n5")) {
            IJ.run("Export Current Image as XML/N5",
                    "  export_path=" + xmlPath);
        } else if ( bdvFormat.equals("h5") ) {
            IJ.run("Export Current Image as XML/HDF5",
                    "  export_path=" + xmlPath );
        }

        // update images.json
        addToImagesJson( imageName, imageType, datasetName );

        // if there's no default json, create one with this image
        File defaultBookmarkJson = new File ( getDefaultBookmarkJsonPath( datasetName ));
        if ( !defaultBookmarkJson.exists() ) {
            createDefaultBookmark( imageName, datasetName );
            writeDefaultBookmarksJson( datasetName );
        }
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

    public Set<String> getCurrentImagesInDefaultBookmark( String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        return currentDefaultBookmarks.get( "default" ).layers.keySet();
    }

    public ImageProperties getImageProperties ( String datasetName, String imageName ) {
        updateCurrentImageProperties( datasetName );
        return currentImagesProperties.get( imageName );
    }

    public String[] getCurrentImages( String datasetName ) {
        updateCurrentImageProperties( datasetName );
        if ( currentImagesProperties.size() > 0 ) {
            Set<String> imageNames = currentImagesProperties.keySet();
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

    private String getDefaultBookmarkJsonPath ( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName, "misc", "bookmarks",
                "default.json");
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

    public boolean isInImages ( String imageName, String datasetName ) {
        return Arrays.stream( getCurrentImages( datasetName ) ).anyMatch(imageName::equals);
    }

    public boolean isInDefaultBookmark ( String imageName, String datasetName ) {
        return getCurrentImagesInDefaultBookmark( datasetName ).contains( imageName );
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

    public void updateCurrentImageProperties(String datasetName ) {
        File imagesJSON = new File( getImagesJsonPath( datasetName ) );

        if ( imagesJSON.exists() ) {
            currentImagesProperties = new ImagesJsonParser( getDatasetPath( datasetName ) ).getImagePropertiesMap();
        } else {
            currentImagesProperties = new HashMap<>();
        }
    }

    public void updateCurrentDefaultBookmarks( String datasetName ) {
        File defaultBookmarkJson = new File ( getDefaultBookmarkJsonPath( datasetName ) );
        if ( defaultBookmarkJson.exists() ) {
            currentDefaultBookmarks = new BookmarksJsonParser( getDatasetPath( datasetName ) ).getDefaultBookmarks();
        } else {
            currentDefaultBookmarks = new HashMap<>();
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
        updateCurrentImageProperties( datasetName );
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

        currentImagesProperties.put( imageName, newImageProperties);

        writeImagesJson( datasetName );
    }

    public void createDefaultBookmark ( String imageName, String datasetName ) {
        updateCurrentImageProperties( datasetName );
        updateCurrentDefaultBookmarks( datasetName );
        HashMap< String, MutableImageProperties> layers = new HashMap<>();
        layers.put( imageName, currentImagesProperties.get(imageName) );

        Bookmark defaultBookmark = new Bookmark();
        defaultBookmark.name = "default";
        defaultBookmark.layers = layers;

        currentDefaultBookmarks.put( "default", defaultBookmark );
    }

    public void addImageToDefaultBookmark( String imageName, String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.put( imageName, currentImagesProperties.get(imageName) );
    }

    public void setImagePropertiesInDefaultBookmark ( String imageName, String datasetName, ImageProperties imageProperties ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.put( imageName, imageProperties );
    }

    public void removeImageFromDefaultBookmark( String imageName, String datasetName ) {
        updateCurrentDefaultBookmarks( datasetName );
        currentDefaultBookmarks.get( "default" ).layers.remove( imageName );
    }

    public void writeDefaultBookmarksJson ( String datasetName ) {
        try {
            new BookmarksJsonParser( getDatasetPath( datasetName) ).saveBookmarksToFile( currentDefaultBookmarks, new File (getDefaultBookmarkJsonPath( datasetName )) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeImagesJson ( String datasetName ) {
        try {
            new ImagesJsonParser( getDatasetPath( datasetName) ).writeImagePropertiesMap( getImagesJsonPath( datasetName),
                    currentImagesProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeDatasetsJson () throws IOException {
        File datasetJSON = new File(dataLocation, "datasets.json");
        new DatasetsParser().datasetsToFile(datasetJSON.getAbsolutePath(), currentDatasets);
    }
}
