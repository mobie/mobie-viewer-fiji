package de.embl.cba.mobie.projects.projectsCreator;

import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;

public class Project {
    private final File projectLocation;
    private final File dataLocation;

    public Project( File projectLocation ) {
        this.projectLocation = projectLocation;
        this.dataLocation = new File( projectLocation, "data");
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
