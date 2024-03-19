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
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// TODO this currently does not work
class RemoteMetadataCreatorTest {

    private ProjectCreator projectCreator;
    private RemoteMetadataCreator remoteMetadataCreator;
    private String datasetName;
    private String imageName;
    private String uiSelectionGroup;
    private String signingRegion;
    private String serviceEndpoint;
    private String bucketName;
    private String datasetJsonPath;

    //@org.junit.jupiter.api.BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        projectCreator = new ProjectCreator( tempDir.toFile() );
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

    void testAddingRemoteMetadata(  ) throws IOException{

        // add image of right type
        projectCreator.getImagesCreator().addImage( ProjectCreatorTestHelper.createImage(imageName, false), imageName,
                datasetName, ProjectCreator.ImageType.Image, new AffineTransform3D(),
                uiSelectionGroup, false );

        ImageDataFormat remoteFormat = ImageDataFormat.OmeZarrS3;

        // add remote metadata
        remoteMetadataCreator.createRemoteMetadata( signingRegion, serviceEndpoint, bucketName, remoteFormat );

        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        assertTrue( dataset.sources().containsKey( imageName ) );
        assertTrue( (( ImageDataSource ) dataset.sources().get( imageName )).imageData.containsKey(remoteFormat) );
    }

    //@Test
    void createRemoteMetadataOmeZarr() throws IOException {
        testAddingRemoteMetadata();
    }
}
