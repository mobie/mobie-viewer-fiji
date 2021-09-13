package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.Dataset;
import de.embl.cba.mobie.serialize.DatasetJsonParser;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.tables.FileAndUrlUtils;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.imageFormatToFolderName;
import static de.embl.cba.mobie.projectcreator.ProjectCreatorTestHelper.makeImage;
import static org.junit.jupiter.api.Assertions.*;

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

    @org.junit.jupiter.api.BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        projectCreator = new ProjectCreator( tempDir.toFile() );
        remoteMetadataCreator = projectCreator.getRemoteMetadataCreator();

        datasetName = "test";
        imageName = "testImage";
        uiSelectionGroup = "testGroup";
        signingRegion = "us-west-2";
        serviceEndpoint = "https://s3.test.de/test/";
        bucketName = "test-bucket";
        projectCreator.getDatasetsCreator().addDataset(datasetName);
        datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "dataset.json" );
    }

    void testAddingRemoteMetadataForCertainFormat( ImageDataFormat imageDataFormat ) throws IOException {

        // add image of right type
        projectCreator.getImagesCreator().addImage( makeImage(imageName), imageName, datasetName, imageDataFormat,
                ProjectCreator.ImageType.image, new AffineTransform3D(), true, uiSelectionGroup);

        ImageDataFormat remoteFormat = null;
        switch( imageDataFormat ) {
            case BdvN5:
                remoteFormat = ImageDataFormat.BdvN5S3;
                break;
            case BdvOmeZarr:
                remoteFormat = ImageDataFormat.BdvOmeZarrS3;
                break;
            case OmeZarr:
                remoteFormat = ImageDataFormat.OmeZarrS3;
                break;
        }

        // add remote metadata
        remoteMetadataCreator.createRemoteMetadata( signingRegion, serviceEndpoint, bucketName, remoteFormat );

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        assertTrue( dataset.sources.containsKey(imageName) );
        assertTrue( dataset.sources.get(imageName).get().imageData.containsKey(remoteFormat) );
        if ( imageDataFormat.hasXml() ) {
            String remoteXmlPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, "images", imageFormatToFolderName( remoteFormat ), imageName + ".xml" );
            assertTrue( new File(remoteXmlPath).exists() );
        }
    }

    @Test
    void createRemoteMetadataBdvN5() throws IOException {
        testAddingRemoteMetadataForCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void createRemoteMetadataBdvOmeZarr() throws IOException {
        testAddingRemoteMetadataForCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void createRemoteMetadataOmeZarr() throws IOException {
        testAddingRemoteMetadataForCertainFormat( ImageDataFormat.OmeZarr);
    }
}