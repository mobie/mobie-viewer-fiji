package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Project {
    private final File projectLocation;
    private final File dataLocation;

    private Datasets currentDatasets;
    private Map<String, Dataset> datasetNameToDataset;

    public Project( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
        this.datasetNameToDataset = new HashMap<>();
        updateDatasets();
    }

    public void updateDatasets() {
        File datasetJSON = new File( getDatasetsJsonPath() );

        if ( datasetJSON.exists() ) {
            currentDatasets = new DatasetsParser().fetchProjectDatasets( dataLocation.getAbsolutePath() );
        } else {
            currentDatasets = new Datasets();
            currentDatasets.datasets = new ArrayList<>();
        }
    }

    public Dataset getDataset( String datasetName ) {
        if ( currentDatasets.datasets.contains(datasetName) ) {
            // Datasets are created lazily, so only create if not already there
            if ( !datasetNameToDataset.containsKey(datasetName) ) {
                datasetNameToDataset.put( datasetName, new Dataset(this, datasetName ));
            }
            return datasetNameToDataset.get(datasetName);
        } else {
            return null;
        }
    }

    public String[] getDatasetNames() {
        if ( currentDatasets.datasets.size() > 0 ) {
            ArrayList<String> datasetNames = currentDatasets.datasets;
            String[] datasetNamesArray = new String[datasetNames.size()];
            datasetNames.toArray( datasetNamesArray );
            return datasetNamesArray;
        } else {
            return new String[] {""};
        }
    }

    public Datasets getCurrentDatasets() {
        return currentDatasets;
    }

    public int getNumberOfDatasets() {
        return currentDatasets.datasets.size();
    }

    public boolean isInDatasets ( String datasetName ) {
        return Arrays.stream( getDatasetNames() ).anyMatch(datasetName::equals);
    }

    public boolean isDefaultDataset( String datasetName ) {
        return currentDatasets.defaultDataset.equals( datasetName );
    }

    public File getDataLocation() {
        return dataLocation;
    }

    public String getDatasetDirectoryPath(String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName);
    }

    public String getImagesDirectoryPath( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName, "images");
    }

    public String getImagesJsonPath( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName,
                "images", "images.json");
    }

    public String getDatasetsJsonPath() {
        File datasetJSON = new File( dataLocation, "datasets.json" );
        return datasetJSON.getAbsolutePath();
    }

    public String getLocalImageXmlPath ( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath(dataLocation.getAbsolutePath(), datasetName, "images", "local", imageName + ".xml");
    }

    public String getTablesDirectoryPath( String datasetName, String imageName ) {
        return FileAndUrlUtils.combinePath( getDatasetDirectoryPath( datasetName ), "tables", imageName);
    }

    public String getDefaultBookmarkJsonPath ( String datasetName ) {
        return FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName, "misc", "bookmarks",
                "default.json");
    }
}
