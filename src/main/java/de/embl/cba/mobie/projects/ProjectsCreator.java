package de.embl.cba.mobie.projects;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.write.BookmarkFileWriter;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.*;
import de.embl.cba.mobie.n5.DownsampleBlock;
import de.embl.cba.mobie.n5.WriteImgPlusToN5;
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
import org.janelia.saalfeldlab.n5.GzipCompression;

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

    public enum BdvFormat {
        // TODO - add OME.ZARR
        n5
    }

    public enum ImageType {
        image,
        segmentation,
        mask
    }

    public enum AddMethod {
        link,
        copy,
        move
    }

    public ProjectsCreator ( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
    }

    private File getImageLocation ( SpimDataMinimal spimDataMinimal, BdvFormat bdvFormat) {
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

    private void writeNewBdvXml ( SpimDataMinimal spimDataMinimal, File imageFile, File saveDirectory, String imageName, BdvFormat bdvFormat ) throws SpimDataException {

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

    private void copyImage ( BdvFormat bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocation( spimDataMinimal, bdvFormat );

        switch ( bdvFormat ) {
            case n5:
                FileUtils.copyDirectory(imageLocation, newImageFile );
                break;
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat);
    }

    private void closeImgLoader ( SpimDataMinimal spimDataMinimal, BdvFormat bdvFormat ) {
        BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();

        switch ( bdvFormat ) {
            case n5:
                N5ImageLoader n5ImageLoader = (N5ImageLoader) imgLoader;
                n5ImageLoader.close();
                break;
        }
    }

    private void moveImage ( BdvFormat bdvFormat, SpimDataMinimal spimDataMinimal, File newXmlDirectory, String imageName ) throws IOException, SpimDataException {
        File newImageFile = new File( newXmlDirectory, imageName + "." + bdvFormat );
        File imageLocation = getImageLocation( spimDataMinimal, bdvFormat );

        // have to explicitly close the image loader, so we can delete the original file
        closeImgLoader( spimDataMinimal, bdvFormat );

        switch ( bdvFormat ) {
            case n5:
                FileUtils.moveDirectory( imageLocation, newImageFile );
                break;
        }

        writeNewBdvXml( spimDataMinimal, newImageFile, newXmlDirectory, imageName, bdvFormat );
    }

    private void updateJsonsForNewImage ( String imageName, ImageType imageType, String datasetName ) {
        // update images.json
        addToImagesJson(imageName, imageType, datasetName);

        // if there's no default json, create one with this image
        File defaultBookmarkJson = new File(getDefaultBookmarkJsonPath(datasetName));
        if (!defaultBookmarkJson.exists()) {
            createDefaultBookmark(imageName, datasetName);
            writeDefaultBookmarksJson(datasetName);
        }
    }

    // TODO - check n5 format
    public void addBdvFormatImage ( File xmlLocation, String datasetName, BdvFormat bdvFormat,
                                    ImageType imageType, AddMethod addMethod ) throws SpimDataException, IOException {
        if ( xmlLocation.exists() ) {
            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load(xmlLocation.getAbsolutePath());
            String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
            File newXmlDirectory = new File(FileAndUrlUtils.combinePath(getImagesPath(datasetName), "local"));
            File newXmlFile = new File(newXmlDirectory, imageName + ".xml");

            if (!newXmlFile.exists()) {
                switch ( addMethod ) {
                    case link:
                        new XmlIoSpimDataMinimal().save(spimDataMinimal, newXmlFile.getAbsolutePath());
                        break;
                    case copy:
                        copyImage(bdvFormat, spimDataMinimal, newXmlDirectory, imageName);
                        break;
                    case move:
                        moveImage( bdvFormat, spimDataMinimal, newXmlDirectory, imageName );
                        break;
                }

                updateJsonsForNewImage(imageName, imageType, datasetName);
            } else {
                Utils.log("Adding image to project failed - this image name already exists");
            }
        } else {
            Utils.log( "Adding image to project failed - xml does not exist" );
        }
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

    public void addImage ( String imageName, String datasetName, BdvFormat bdvFormat, ImageType imageType,
                           AffineTransform3D sourceTransform, boolean useDefaultSettings ) {
        ImagePlus imp = IJ.getImage();
        String xmlPath = getLocalImageXmlPath( datasetName, imageName);
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

    public String getLocalImageXmlPath ( String datasetName, String imageName ) {
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
            currentDefaultBookmarks = new BookmarkReader( getDatasetPath( datasetName ) ).readDefaultBookmarks();
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

    public void addToImagesJson ( String imageName, ImageType imageType, String datasetName ) {
        updateCurrentImageProperties( datasetName );
        ImageProperties newImageProperties = new ImageProperties();
        newImageProperties.type = imageType.toString();

        switch( imageType ) {
            case segmentation:
                newImageProperties.color = ColoringLuts.GLASBEY;
                newImageProperties.tableFolder = "tables/" + imageName;
                addDefaultTableForImage( imageName, datasetName );
                break;
            default:
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
            BookmarkFileWriter.saveBookmarksToFile( currentDefaultBookmarks, new File (getDefaultBookmarkJsonPath( datasetName )) );
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
