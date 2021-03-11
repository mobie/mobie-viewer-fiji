package de.embl.cba.mobie.projects.projectsCreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.N5S3ImageLoader;
import de.embl.cba.mobie.n5.S3Authentication;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.ImgLoader;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.IOException;

import static de.embl.cba.mobie.utils.ExportUtils.getBdvFormatFromSpimDataMinimal;

public class RemoteMetadataCreator {
    Project project;
    String signingRegion;
    String serviceEndpoint;
    String bucketName;
    S3Authentication authentication;

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

    private boolean addRemoteMetadataForImage( String datasetName, String imageName ) throws SpimDataException, IOException {
        String localXmlLocation = project.getLocalImageXmlPath( datasetName, imageName );
        String remoteXmlLocation = project.getRemoteImagesDirectoryPath( datasetName );

        SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( localXmlLocation );
        ProjectsCreator.BdvFormat bdvFormat = getBdvFormatFromSpimDataMinimal( spimDataMinimal );

        if ( bdvFormat == null ) {
            Utils.log( "Image: " + imageName + " in dataset:" + datasetName + " is of an unsupported format. \n" +
                    "Aborting, and removing all remote metadata" );
            return false;
        } else {
            ImgLoader imgLoader = null;

            switch (bdvFormat) {
                case n5:
                    String key = datasetName + "/images/remote/" + imageName + ".n5";
                    imgLoader = new N5S3ImageLoader( serviceEndpoint, signingRegion, bucketName, key,
                            authentication, spimDataMinimal.getSequenceDescription() );
                    break;
            }

            spimDataMinimal.setBasePath( new File( remoteXmlLocation ) );
            spimDataMinimal.getSequenceDescription().setImgLoader(imgLoader);
            new XmlIoSpimDataMinimal().save(spimDataMinimal, new File(remoteXmlLocation, imageName + ".xml").getAbsolutePath());
        }

        return true;
    }

    private boolean addRemoteMetadataForDataset( String datasetName ) throws SpimDataException, IOException {
        for ( String imageName: project.getDataset( datasetName ).getImageNames() ) {
            if ( !imageName.equals("") ) {
                Utils.log("Adding metadata for image: " + imageName );
                boolean addedSuccessfully = addRemoteMetadataForImage( datasetName, imageName );
                if ( !addedSuccessfully ) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: project.getDatasetNames() ) {
            if ( !datasetName.equals("") ) {
                Utils.log("Adding metadata for dataset: " + datasetName );
                boolean addedSuccessfully = addRemoteMetadataForDataset( datasetName );
                if ( !addedSuccessfully ) {
                    return false;
                }
            }
        }
        return true;
    }

    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName,
                                      S3Authentication authentication ) {
        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.authentication = authentication;

        try {
            deleteAllRemoteMetadata();

            try {
                // If some issue occurs, delete the remote metadata
                if (!addAllRemoteMetadata()) {
                    deleteAllRemoteMetadata();
                }
            } catch (SpimDataException | IOException e) {
                Utils.log( "Error - aborting, and removing all remote metadata" );
                deleteAllRemoteMetadata();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
