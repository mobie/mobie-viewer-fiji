package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DatasetsCreator {

    // location of 'data' folder of project
    private Project project;
    private Datasets currentDatasets;

    public DatasetsCreator( Project project ) {
        this.project = project;
    }

    private void updateCurrentDatasets() {
        File datasetJSON = new File( project.getDatasetsJsonPath() );

        if ( datasetJSON.exists() ) {
            currentDatasets = new DatasetsParser().fetchProjectDatasets( project.getDataLocation().getAbsolutePath() );
        } else {
            currentDatasets = new Datasets();
            currentDatasets.datasets = new ArrayList<>();
        }
    }

    public String[] getCurrentDatasetNames() {
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

    public void addDataset ( String datasetName ) {
        File datasetDir = new File ( project.getDatasetDirectoryPath( datasetName ) );
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
                currentDatasets.defaultDataset = datasetName;
            }
            currentDatasets.datasets.add(datasetName);
            try {
                writeDatasetsJson();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Utils.log( "Dataset creation failed - this name already exists" );
        }

    }

    public void renameDataset( String oldName, String newName ) {
        updateCurrentDatasets();

        File oldDatasetDir = new File ( project.getDatasetDirectoryPath(oldName) );
        File newDatasetDir = new File ( project.getDatasetDirectoryPath(newName) );

        if ( oldDatasetDir.exists() ) {
            if ( oldDatasetDir.renameTo(newDatasetDir)) {
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

    public void makeDefaultDataset ( String datasetName ) {
        updateCurrentDatasets();

        currentDatasets.defaultDataset = datasetName;
        try {
            writeDatasetsJson();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isInDatasets ( String datasetName ) {
        return Arrays.stream( getCurrentDatasetNames() ).anyMatch(datasetName::equals);
    }

    public boolean isDefaultDataset( String datasetName ) {
        updateCurrentDatasets();
        return currentDatasets.defaultDataset.equals( datasetName );
    }

    public void writeDatasetsJson () throws IOException {
        new DatasetsParser().datasetsToFile( project.getDatasetsJsonPath(), currentDatasets);
    }

}
