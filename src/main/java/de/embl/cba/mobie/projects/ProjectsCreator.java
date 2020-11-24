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

    public void addDataset ( String name ) {
        File datasetDir = new File ( dataLocation, name );
        if ( !datasetDir.exists() ) {
            datasetDir.mkdirs();
        }

        File datasetJSON = new File( dataLocation, "datasets.json");

        // if this is the first dataset, then make this the default
        if ( currentDatasets.datasets.size() == 0) {
            currentDatasets.defaultDataset = name;
        }
        currentDatasets.datasets.add( name );
        try {
            new DatasetsParser().datasetsToFile( datasetJSON.getAbsolutePath(), currentDatasets );
        } catch (IOException e) {
            e.printStackTrace();
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
