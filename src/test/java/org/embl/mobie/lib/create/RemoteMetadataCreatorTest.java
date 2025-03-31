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

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.embl.mobie.lib.create.ProjectCreatorTestHelper.createImage;
import static org.junit.jupiter.api.Assertions.*;

class RemoteMetadataCreatorTest {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;
    private RemoteMetadataCreator remoteMetadataCreator;
    private String datasetName;
    private String imageName;
    private String uiSelectionGroup;
    private String signingRegion;
    private String serviceEndpoint;
    private String bucketName;
    private String datasetJsonPath;
    private File tempDir;

    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        this.tempDir = tempDir.toFile();
        // Write project into sub-folder called 'data'
        File projectDir = new File(this.tempDir, "data");
        projectCreator = new ProjectCreator( projectDir );
        remoteMetadataCreator = projectCreator.getRemoteMetadataCreator();

        datasetName = "test";
        imageName = "testImage";
        uiSelectionGroup = "testGroup";
        signingRegion = "us-west-2";
        serviceEndpoint = "https://s3.test.de/test/";
        bucketName = "test-bucket";
        projectCreator.getDatasetsCreator().addDataset(datasetName, false);
        datasetJsonPath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "dataset.json" );
    }

    void assertionsForRemoteMetadata(  ) throws IOException{
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        assertTrue( dataset.sources().containsKey( imageName ) );

        ImageDataSource source = ( ImageDataSource ) dataset.sources().get( imageName );
        assertTrue( source.imageData.containsKey(ImageDataFormat.OmeZarrS3) );

        // Check s3 address is set correctly in remote metadata
        StorageLocation remoteStorageLocation = source.imageData.get(ImageDataFormat.OmeZarrS3);
        String localRelativePath = source.imageData.get(ImageDataFormat.OmeZarr).relativePath;
        assertEquals(remoteStorageLocation.signingRegion, signingRegion);
        assertEquals(remoteStorageLocation.s3Address,
                serviceEndpoint + bucketName + "/" + datasetName + "/" + localRelativePath);
    }

    @Test
    void createRemoteMetadataOmeZarr() throws IOException {
        // add ome-zarr image
        projectCreator.getImagesCreator().addImage(
                createImage(imageName, false),
                imageName,
                datasetName,
                ProjectCreator.ImageType.Image,
                new AffineTransform3D(),
                uiSelectionGroup,
                false,
                false );

        // add remote metadata
        remoteMetadataCreator.createOMEZarrRemoteMetadata( signingRegion, serviceEndpoint, bucketName );
        assertionsForRemoteMetadata();
    }

    @Test
    void createRemoteMetadataWithImagesOutsideProject() throws IOException {
        // create ome-zarr image outside of project
        String filePath = new File(tempDir, imageName + ".ome.zarr").getAbsolutePath();
        OMEZarrWriter.write(
                createImage( imageName, false ),
                filePath,
                OMEZarrWriter.ImageType.Intensities,
                false
        );

        // link to the ome-zarr image
        projectCreator.getImagesCreator().linkOrCopyOMEZarrImage( filePath, imageName, datasetName,
                ProjectCreator.ImageType.Image, ProjectCreator.AddMethod.Link,
                uiSelectionGroup, false, false );

        // try to add remote metadata
        remoteMetadataCreator.createOMEZarrRemoteMetadata( signingRegion, serviceEndpoint, bucketName );

        // check that it detected the image was outside the project, and therefore didn't write the remote metadata
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        assertTrue( dataset.sources().containsKey( imageName ) );
        ImageDataSource source = ( ImageDataSource ) dataset.sources().get( imageName );
        assertFalse( source.imageData.containsKey(ImageDataFormat.OmeZarrS3) );
    }
}
