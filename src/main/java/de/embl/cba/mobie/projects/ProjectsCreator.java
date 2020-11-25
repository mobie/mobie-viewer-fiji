package de.embl.cba.mobie.projects;

import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ProjectsCreator {
    private final File projectLocation;
    private final File dataLocation;
    private Datasets currentDatasets;

    public ProjectsCreator ( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
        new ProjectsCreatorPanel( this );
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

    public String[] getCurrentDatasets () {
        File datasetJSON = new File( dataLocation, "datasets.json");

        if ( datasetJSON.exists() ) {
            currentDatasets = new DatasetsParser().fetchProjectDatasets(dataLocation.getAbsolutePath());
            ArrayList<String> datasetNames = currentDatasets.datasets;

            String[] datasetNamesArray = new String[datasetNames.size()];
            datasetNames.toArray( datasetNamesArray );
            return datasetNamesArray;
        } else {
            currentDatasets = new Datasets();
            currentDatasets.datasets = new ArrayList<>();
            return new String[] {""};
        }
    }


}
