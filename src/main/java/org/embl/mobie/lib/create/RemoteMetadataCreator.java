/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.create;

import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to create and modify the metadata required for remote (S3) storage of MoBIE projects
 */
public class RemoteMetadataCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    ProjectCreator projectCreator;
    String signingRegion;
    String serviceEndpoint;
    String bucketName;
    ImageDataFormat remoteImageDataFormat;
    ImageDataFormat localImageDataFormat;

    /**
     * Make a remoteMetadataCreator - includes all functions for creation/modification of remote metadata
     * @param projectCreator projectCreator
     */
    public RemoteMetadataCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private void deleteAllRemoteMetadata() throws IOException {
        for ( String datasetName: projectCreator.getProject().datasets() ) {
            if ( !datasetName.equals("") ) {
                Dataset dataset = projectCreator.getDataset( datasetName );
                for ( String imageName: dataset.sources().keySet() ) {
                    deleteRemoteMetadataForImage( datasetName, imageName );
                }
                projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
            }
        }
    }

    private void deleteRemoteMetadataForImage( String datasetName, String imageName ) throws IOException {
        ImageDataSource imageSource = ( ImageDataSource ) projectCreator.getDataset( datasetName ).sources().get( imageName );
        if ( imageSource.imageData.containsKey( remoteImageDataFormat ) ) {

            if ( remoteImageDataFormat.hasXml() ) {
                // delete any existing remote metadata .xml files
                File currentRemoteXmlLocation = new File(IOHelper.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                        datasetName, imageSource.imageData.get(remoteImageDataFormat).relativePath));
                if (currentRemoteXmlLocation.exists()) {
                    if (!currentRemoteXmlLocation.delete()) {
                        String errorMessage = "Remote metadata for: " + imageName + " in dataset: " + datasetName + " could not be deleted.";
                        IJ.log(errorMessage);
                        throw new IOException(errorMessage);
                    }
                }
            }
            imageSource.imageData.remove( remoteImageDataFormat );
        }
    }

    private void addRemoteMetadataForImage( String datasetName, String imageName )
    {
        ImageDataSource imageSource = ( ImageDataSource ) projectCreator.getDataset( datasetName ).sources().get( imageName );
        if ( !imageSource.imageData.containsKey( localImageDataFormat ) ) {
            IJ.log( "No images of format " + localImageDataFormat + " for " + imageName +
                    " in dataset:" + datasetName + ". Skipping this image." );
            return;
        }

        // give absolute s3 path to ome.zarr file
        StorageLocation storageLocation = new StorageLocation();
        String relativePath = imageSource.imageData.get(localImageDataFormat).relativePath;
        storageLocation.s3Address = serviceEndpoint + bucketName + "/" + datasetName + "/" + relativePath;
        storageLocation.signingRegion = signingRegion;
        imageSource.imageData.put( remoteImageDataFormat, storageLocation );
    }

    private void addRemoteMetadataForDataset( String datasetName )  {
        Dataset dataset = projectCreator.getDataset( datasetName );
        for ( String imageName: dataset.sources().keySet() ) {
            if ( !imageName.equals("") ) {
                IJ.log("Adding metadata for image: " + imageName );
                addRemoteMetadataForImage( datasetName, imageName );
            }
        }

        projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
    }

    private void addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: projectCreator.getProject().datasets() ) {
            if ( !datasetName.equals("") ) {
                IJ.log("Adding metadata for dataset: " + datasetName );
                addRemoteMetadataForDataset( datasetName );
            }
        }
        IJ.log( "Done." );
    }

    /**
     * Add remote metadata. Note this will overwrite any existing remote metadata for the given image format.
     * @param signingRegion signing region e.g. us-west-2
     * @param serviceEndpoint service endpoint e.g. https://s3.embl.de
     * @param bucketName bucket name
     * @param imageDataFormat image format
     */
    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName, ImageDataFormat imageDataFormat ) {

        if ( !imageDataFormat.isRemote() ) {
            IJ.log( "Creating remote metadata aborted - provided image data format is not remote." );
        }

        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.remoteImageDataFormat = imageDataFormat;

        if ( this.signingRegion.equals("") ) {
            this.signingRegion = null;
        }

        if ( !this.serviceEndpoint.endsWith("/") ) {
            this.serviceEndpoint = this.serviceEndpoint + "/";
        }

        switch( remoteImageDataFormat ) {
            case BdvN5S3:
                localImageDataFormat = ImageDataFormat.BdvN5;
                break;
            case BdvOmeZarrS3:
                localImageDataFormat = ImageDataFormat.BdvOmeZarr;
                break;
            case OmeZarrS3:
                localImageDataFormat = ImageDataFormat.OmeZarr;
                break;
        }

        try {
            // clean any old remote metadata
            deleteAllRemoteMetadata();

            try {
               addAllRemoteMetadata();
            } catch (SpimDataException | IOException e) {
                IJ.log( "Error - aborting, and removing all " + remoteImageDataFormat + " remote metadata" );
                deleteAllRemoteMetadata();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
