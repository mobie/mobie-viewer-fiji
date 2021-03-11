package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;

public class DatasetsCreator {

    private Project project;

    public DatasetsCreator( Project project ) {
        this.project = project;
    }

    public void addDataset ( String datasetName ) {
        boolean contains = project.isInDatasets(datasetName);
        if (!contains) {
            File datasetDir = new File ( project.getDatasetDirectoryPath( datasetName ) );

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
                Datasets currentDatasets = project.getCurrentDatasets();
                if ( currentDatasets.datasets.size() == 0) {
                    currentDatasets.defaultDataset = datasetName;
                }
                currentDatasets.datasets.add(datasetName);
                try {
                    writeDatasetsJson( currentDatasets );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Utils.log( "Dataset creation failed - this name already exists" );
            }
        } else {
            Utils.log("Add dataset failed - dataset already exists");
        }
    }

    public void renameDataset( String oldName, String newName ) {

        if ( !newName.equals(oldName) ) {
            // check not already in datasets
            boolean contains = project.isInDatasets(newName);
            if (!contains) {
                File oldDatasetDir = new File(project.getDatasetDirectoryPath(oldName));
                File newDatasetDir = new File(project.getDatasetDirectoryPath(newName));

                if (oldDatasetDir.exists()) {
                    if (oldDatasetDir.renameTo(newDatasetDir)) {

                        Datasets currentDatasets = project.getCurrentDatasets();
                        // update json
                        if (currentDatasets.defaultDataset.equals(oldName)) {
                            currentDatasets.defaultDataset = newName;
                        }

                        int indexOld = currentDatasets.datasets.indexOf(oldName);
                        currentDatasets.datasets.set(indexOld, newName);

                        try {
                            writeDatasetsJson(currentDatasets);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Utils.log("Rename directory failed");
                    }
                } else {
                    Utils.log("Rename dataset failed - that dataset doesn't exist");
                }
            } else {
                Utils.log( "Rename dataset failed - there is already a dataset of that name" );
            }
        } else {
            Utils.log("Rename dataset failed - new name is the same as old name");
        }

    }

    public void makeDefaultDataset ( String datasetName ) {
        Datasets currentDatasets = project.getCurrentDatasets();
        currentDatasets.defaultDataset = datasetName;
        try {
            writeDatasetsJson( currentDatasets );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void writeDatasetsJson( Datasets datasets ) throws IOException {
        new DatasetsParser().datasetsToFile( project.getDatasetsJsonPath(), datasets);
    }

}
