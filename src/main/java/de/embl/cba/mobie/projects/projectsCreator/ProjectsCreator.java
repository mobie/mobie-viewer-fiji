package de.embl.cba.mobie.projects.projectsCreator;

import java.io.File;

public class ProjectsCreator {

    private final Project project;
    private final DatasetsCreator datasetsCreator;
    private final ImagesCreator imagesCreator;
    private final ImagesJsonCreator imagesJsonCreator;
    private final DefaultBookmarkCreator defaultBookmarkCreator;
    private final RemoteMetadataCreator remoteMetadataCreator;

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

    public enum Authentication {
        Anonymous,
        Protected
    }

    public ProjectsCreator ( File projectLocation ) {
        this.project = new Project( projectLocation );
        this.datasetsCreator = new DatasetsCreator( project );
        this.imagesJsonCreator = new ImagesJsonCreator( project );
        this.defaultBookmarkCreator = new DefaultBookmarkCreator( project );
        this.imagesCreator = new ImagesCreator( project, imagesJsonCreator, defaultBookmarkCreator );
        this.remoteMetadataCreator = new RemoteMetadataCreator( project );
    }

    public Project getProject() {
        return project;
    }

    public DatasetsCreator getDatasetsCreator() {
        return datasetsCreator;
    }

    public ImagesCreator getImagesCreator() {
        return imagesCreator;
    }

    public ImagesJsonCreator getImagesJsonCreator() {
        return imagesJsonCreator;
    }

    public DefaultBookmarkCreator getDefaultBookmarkCreator() {
        return defaultBookmarkCreator;
    }

    public RemoteMetadataCreator getRemoteMetadataCreator() {
        return remoteMetadataCreator;
    }
}
