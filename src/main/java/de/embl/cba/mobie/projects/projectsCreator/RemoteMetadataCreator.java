package de.embl.cba.mobie.projects.projectsCreator;

import org.apache.commons.io.FileUtils;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.IOException;

public class RemoteMetadataCreator {
    Project project;

    public RemoteMetadataCreator( Project project ) {
        this.project = project;
    }

    private void deleteAllRemoteMetadata() throws IOException {
        for ( String datasetName: project.getDatasetNames() ) {
            if ( !datasetName.equals("") ) {
                File remoteDir = new File( project.getRemoteImagesDirectoryPath( datasetName ) );
                FileUtils.cleanDirectory( remoteDir );
            }
        }
    }

    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName,
                                      ProjectsCreator.Authentication authentication ) {
        try {
            deleteAllRemoteMetadata();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
