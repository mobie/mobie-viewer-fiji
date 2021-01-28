package de.embl.cba.mobie.projects;

import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
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

import javax.swing.*;
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
    // holds default bookmarks for one chosen dataset
    private Map<String, Bookmark> currentDefaultBookmarks;

    public ProjectsCreator ( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
    }

    private File getImageLocation ( SpimDataMinimal spimDataMinimal, String bdvFormat) {
        File imageLocation = null;
        if ( bdvFormat.equals("n5") ) {
            // get image loader to find absolute image location
            N5ImageLoader n5ImageLoader = (N5ImageLoader) spimDataMinimal.getSequenceDescription().getImgLoader();
            imageLocation = n5ImageLoader.getN5File();
        } else if ( bdvFormat.equals("h5") ) {
            Hdf5ImageLoader h5ImageLoader = (Hdf5ImageLoader) spimDataMinimal.getSequenceDescription().getImgLoader();
            imageLocation = h5ImageLoader.getHdf5File();
        }

        return imageLocation;
    }

    private void writeNewBdvXml ( SpimDataMinimal spimDataMinimal, File imageFile, File saveDirectory, String imageName, String bdvFormat ) throws SpimDataException {

        ImgLoader imgLoader = null;
        if ( bdvFormat.equals("n5") ) {
            imgLoader = new N5ImageLoader( imageFile, null);

        } else if ( bdvFormat.equals("h5") ) {
            imgLoader = new Hdf5ImageLoader( imageFile, null,null, false);
        }

        spimDataMinimal.setBasePath( saveDirectory );
        spimDataMinimal.getSequenceDescription().setImgLoader(imgLoader);
        new XmlIoSpimDataMinimal().save(spimDataMinimal, new File( saveDirectory, imageName + ".xml").getAbsolutePath() );
    }

    private void copyImage ( String bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocation( spimDataMinimal, bdvFormat );
        if ( bdvFormat.equals("n5") ) {
            FileUtils.copyDirectory(imageLocation, newImageFile );
        } else if (bdvFormat.equals("h5") ) {
            FileUtils.copyFile( imageLocation, newImageFile );
        }
        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat);
    }

    private void closeImgLoader ( SpimDataMinimal spimDataMinimal, String bdvFormat ) {
        BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();
        if ( bdvFormat.equals("n5") ) {
            N5ImageLoader n5ImageLoader = (N5ImageLoader) imgLoader;
            n5ImageLoader.close();
        } else if ( bdvFormat.equals("h5") ) {
            Hdf5ImageLoader h5ImageLoader = (Hdf5ImageLoader) imgLoader;
            h5ImageLoader.close();
        }
    }

    private void moveImage ( String bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocation( spimDataMinimal, bdvFormat );

        // have to explicitly close the image loader, so we can delete the original file
        closeImgLoader( spimDataMinimal, bdvFormat );

        if ( bdvFormat.equals("n5") ) {
            FileUtils.moveDirectory( imageLocation, newImageFile );
        } else if (bdvFormat.equals("h5") ) {
            FileUtils.moveFile( imageLocation, newImageFile );
        }
        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat );
    }

    private void updateJsonsForNewImage ( String imageName, String imageType, String datasetName ) {
        // update images.json
        addToImagesJson(imageName, imageType, datasetName);

        // if there's no default json, create one with this image
        File defaultBookmarkJson = new File(getDefaultBookmarkJsonPath(datasetName));
        if (!defaultBookmarkJson.exists()) {
            createDefaultBookmark(imageName, datasetName);
            writeDefaultBookmarksJson(datasetName);
        }
    }

    public void addBdvFormatImage ( File xmlLocation, String datasetName, String bdvFormat, String imageType, String addMethod ) throws SpimDataException, IOException {
        if ( xmlLocation.exists() ) {
            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load(xmlLocation.getAbsolutePath());
            String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
            File newXmlDirectory = new File(FileAndUrlUtils.combinePath(getImagesPath(datasetName), "local"));
            File newXmlFile = new File(newXmlDirectory, imageName + ".xml");

            if (!newXmlFile.exists()) {
                if (addMethod.equals("link to current image location")) {
                    new XmlIoSpimDataMinimal().save(spimDataMinimal, newXmlFile.getAbsolutePath());
                } else if (addMethod.equals("copy image")) {
                    copyImage(bdvFormat, spimDataMinimal, newXmlDirectory, imageName);
                } else if ( addMethod.equals("move image") ) {
                    moveImage( bdvFormat, spimDataMinimal, newXmlDirectory, imageName );
                }
                updateJsonsForNewImage(imageName, imageType, datasetName);
            } else {
                Utils.log("Adding image to project failed - this image name already exists");
            }
        } else {
            Utils.log( "Adding image to project failed - xml does not exist" );
        }
    }

    public String getDefaultAffineForCurrentImage () {
        ImagePlus imp = IJ.getImage();
        final double pixelWidth = imp.getCalibration().pixelWidth;
        final double pixelHeight = imp.getCalibration().pixelHeight;
        final double pixelDepth = imp.getCalibration().pixelDepth;

        String defaultAffine = pixelWidth + " 0.0 0.0 0.0 0.0 " + pixelHeight + " 0.0 0.0 0.0 0.0 " + pixelDepth + " 0.0";
        return defaultAffine;
    }

    private void addAffineTransformToXml ( String xmlPath, String affineTransform ) throws SpimDataException {
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
    }

    public void addImage ( String imageName, String datasetName, String bdvFormat, String imageType, String affineTransform ) {
        String xmlPath = FileAndUrlUtils.combinePath(projectLocation.getAbsolutePath(), "data", datasetName, "images", "local", imageName + ".xml");
        File xmlFile = new File( xmlPath );

        if ( !xmlFile.exists() ) {
            if (bdvFormat.equals("n5")) {
                IJ.run("Export Current Image as XML/N5",
                        "  export_path=" + xmlPath);
            } else if (bdvFormat.equals("h5")) {
                IJ.run("Export Current Image as XML/HDF5",
                        "  export_path=" + xmlPath);
            }

            // check image written successfully, before writing jsons
            if ( xmlFile.exists() ) {
                // if an affine transform is provided, re-open the xml and add the affine
                if ( affineTransform != null ) {
                    try {
                        addAffineTransformToXml( xmlPath, affineTransform );
                    } catch (SpimDataException e) {
                        Utils.log( "Error adding affine transform to xml file. Check xml manually.");
                        e.printStackTrace();
                    }

                }
                updateJsonsForNewImage(imageName, imageType, datasetName);
            }
        } else {
            Utils.log( "Adding image to project failed - this image name already exists" );
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
        } else {
            Utils.log( "Dataset creation failed - this name already exists" );
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
        updateCurrentDatasets();
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
                Utils.log( "Rename directory failed" );
            }
        } else {
            Utils.log( "Rename dataset failed - that dataset doesn't exist" );
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

            Utils.log( " Creating default table... 0 label is counted as background" );

            String[] columnNames = {"label_id", "anchor_x", "anchor_y",
                    "anchor_z", "bb_min_x", "bb_min_y", "bb_min_z", "bb_max_x",
                    "bb_max_y", "bb_max_z"};

            final LazySpimSource labelsSource = new LazySpimSource("labelImage", getLocalImageXmlPath(datasetName, imageName));

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
