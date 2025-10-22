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
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;

import java.io.File;
import java.io.IOException;

import static org.embl.mobie.lib.create.ProjectCreatorHelper.uriIsInsideDir;

/**
 * Class to create and modify the metadata required for remote (S3) storage of MoBIE projects.
 * Only supports ome-zarr.
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
     * Make a remoteMetadataCreator - includes all functions for creation/modification of ome-zarr remote metadata
     * @param projectCreator projectCreator
     */
    public RemoteMetadataCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private void deleteAllRemoteMetadata() throws IOException {
        for ( String datasetName: projectCreator.getProject().datasets() ) {
            if ( !datasetName.isEmpty() ) {
                Dataset dataset = projectCreator.getDataset( datasetName );
                for ( String imageName: dataset.sources().keySet() ) {
                    deleteRemoteMetadataForImage( datasetName, imageName );
                }
                projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
            }
        }
    }

    private void deleteRemoteMetadataForImage( String datasetName, String imageName ) {
        ImageDataSource imageSource = ( ImageDataSource ) projectCreator.getDataset( datasetName ).sources().get( imageName );
        if ( imageSource.imageData.containsKey( remoteImageDataFormat ) ) {
            imageSource.imageData.remove( remoteImageDataFormat );
        }
    }

    private void addRemoteMetadataForImage( String datasetName, String imageName ) throws IOException {
        ImageDataSource imageSource = ( ImageDataSource ) projectCreator.getDataset( datasetName ).sources().get( imageName );
        if ( !imageSource.imageData.containsKey( localImageDataFormat ) ) {
            IJ.log( "No images of format " + localImageDataFormat + " for " + imageName +
                    " in dataset:" + datasetName + ". Skipping this image." );
            return;
        }

        String relativePath = imageSource.imageData.get(localImageDataFormat).relativePath;
        String absolutePath = imageSource.imageData.get(localImageDataFormat).absolutePath;

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.signingRegion = signingRegion;

        // Handle images with remote S3 paths
        if ( absolutePath != null && IOHelper.getType(absolutePath) != IOHelper.ResourceType.FILE ) {
            storageLocation.s3Address = absolutePath;
            imageSource.imageData.put(remoteImageDataFormat, storageLocation);
            return;
        }

        // Don't allow images with absolute local file paths - i.e. those linking to local locations outside
        // the project.
        if ( relativePath == null ) {
            String errorMesage = "Image: " + imageName + " for dataset:" + datasetName + " has no relative path. \n" +
                    "You can't 'link' to images outside the project folder, when uploading to s3";
            IJ.log( errorMesage );
            throw new IOException( errorMesage );
        }

        // Don't allow images with relative paths to locations outside the project directory. These won't work when the
        // project is uploaded to S3.
        File imageLocation = new File(
                IOHelper.combinePath(
                        projectCreator.getProjectLocation().getAbsolutePath(),
                        datasetName,
                        relativePath)
        );
        if (!uriIsInsideDir(imageLocation.getAbsolutePath(), projectCreator.getProjectLocation())) {
            String errorMessage = "Image: " + imageName + " for dataset:" + datasetName + " is not in project folder. \n" +
                    "You can't 'link' to images outside the project folder, when uploading to s3";
            IJ.log( errorMessage );
            throw new IOException( errorMessage );
        }

        // give absolute s3 path to ome.zarr file
        storageLocation.s3Address = serviceEndpoint + bucketName + "/" + datasetName + "/" + relativePath;
        imageSource.imageData.put( remoteImageDataFormat, storageLocation );
    }

    private void addRemoteMetadataForDataset( String datasetName ) throws IOException {
        Dataset dataset = projectCreator.getDataset( datasetName );
        for ( String imageName: dataset.sources().keySet() ) {
            if ( !imageName.isEmpty() ) {
                IJ.log("Adding metadata for image: " + imageName );
                addRemoteMetadataForImage( datasetName, imageName );
            }
        }

        projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
    }

    private void addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: projectCreator.getProject().datasets() ) {
            if ( !datasetName.isEmpty() ) {
                IJ.log("Adding metadata for dataset: " + datasetName );
                addRemoteMetadataForDataset( datasetName );
            }
        }
        IJ.log( "Done." );
    }

    /**
     * Add ome-zarr remote metadata. Note this will overwrite any existing remote metadata.
     * @param signingRegion signing region e.g. us-west-2
     * @param serviceEndpoint service endpoint e.g. https://s3.embl.de
     * @param bucketName bucket name
     */
    public void createOMEZarrRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName ) {

        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.remoteImageDataFormat = ImageDataFormat.OmeZarrS3;
        this.localImageDataFormat = ImageDataFormat.OmeZarr;

        if ( this.signingRegion.isEmpty() ) {
            this.signingRegion = null;
        }

        if ( !this.serviceEndpoint.endsWith("/") ) {
            this.serviceEndpoint = this.serviceEndpoint + "/";
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
