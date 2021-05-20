package de.embl.cba.mobie2.projectcreator;

import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.Project;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.serialize.ProjectJsonParser;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ProjectCreator {

    private final File dataLocation;

    private final File projectJson;
    private Project project;

    private File currentDatasetJson;
    private String currentDatasetName;
    private Dataset currentDataset;

    private final DatasetsCreator datasetsCreator;
    private final ImagesCreator imagesCreator;
    private final DatasetJsonCreator datasetJsonCreator;
    private final RemoteMetadataCreator remoteMetadataCreator;

    public enum BdvFormat {
        // TODO - add OME.ZARR
        n5
    }

    public enum ImageType {
        image,
        segmentation
    }

    public enum AddMethod {
        link,
        copy,
        move
    }

    // data location is the folder that contains the projects.json and the individual dataset folders
    public ProjectCreator(File dataLocation ) throws IOException {
        this.dataLocation = dataLocation;
        projectJson = new File( FileAndUrlUtils.combinePath(  dataLocation.getAbsolutePath(), "project.json") );
        if ( projectJson.exists() ) {
            reloadProject();
        } else {
            this.project = new Project();
            project.setDatasets( new ArrayList<>() );
            project.setSpecVersion( "0.2.0" );
        }

        this.datasetsCreator = new DatasetsCreator( this );
        this.datasetJsonCreator = new DatasetJsonCreator( this );
        this.imagesCreator = new ImagesCreator( this );
        this.remoteMetadataCreator = new RemoteMetadataCreator( this );
    }

    public File getDataLocation() { return dataLocation; }

    public Project getProject() {
        return project;
    }

    public File getProjectJson() { return projectJson; }

    public void reloadProject() throws IOException {
        this.project = new ProjectJsonParser().parseProject( projectJson.getAbsolutePath() );
    }

    public Dataset getDataset( String datasetName ) {
        if ( datasetName.equals(currentDatasetName) ) {
            return currentDataset;
        } else {
            File datasetJson = new File( FileAndUrlUtils.combinePath( dataLocation.getAbsolutePath(), datasetName, "dataset.json") );
            if ( datasetJson.exists() ) {
                try {
                    currentDatasetJson = datasetJson;
                    currentDatasetName = datasetName;
                    reloadCurrentDataset();
                    return currentDataset;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public void reloadCurrentDataset() throws IOException {
        this.currentDataset = new DatasetJsonParser().parseDataset( currentDatasetJson.getAbsolutePath() );
    }

    public DatasetsCreator getDatasetsCreator() {
        return datasetsCreator;
    }

    public ImagesCreator getImagesCreator() {
        return imagesCreator;
    }

    public DatasetJsonCreator getDatasetJsonCreator() {
        return datasetJsonCreator;
    }

    public RemoteMetadataCreator getRemoteMetadataCreator() {
        return remoteMetadataCreator;
    }
}
