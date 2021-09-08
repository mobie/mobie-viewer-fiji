package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.Dataset;
import de.embl.cba.mobie.serialize.DatasetJsonParser;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.imageFormatToFolderName;
import static org.junit.jupiter.api.Assertions.*;

class ImagesCreatorTest {

    private ProjectCreator projectCreator;
    private ImagesCreator imagesCreator;
    private ImagePlus imp;
    private String imageName;
    private String datasetName;
    private AffineTransform3D sourceTransform;
    private boolean useDefaultSettings;
    private String uiSelectionGroup;
    private String datasetJsonPath;

    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        projectCreator = new ProjectCreator( tempDir.toFile() );
        imagesCreator = projectCreator.getImagesCreator();

        imageName = "testImage";
        datasetName = "test";
        uiSelectionGroup = "testGroup";
        sourceTransform = new AffineTransform3D();
        useDefaultSettings = true;

        projectCreator.getDatasetsCreator().addDataset(datasetName);

        // make an image with random values, same size as the imagej sample head image
        imp = IJ.createImage(imageName, "8-bit noise", 186, 226, 27);

        datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "dataset.json" );
    }

    void testAddingImageInCertainFormat( ImageDataFormat imageDataFormat, List<String> filePaths ) throws IOException {
        imagesCreator.addImage( imp, imageName, datasetName,
                imageDataFormat, ProjectCreator.ImageType.image,
                sourceTransform, useDefaultSettings, uiSelectionGroup );

        assertTrue( new File(datasetJsonPath).exists() );
        for ( String filePath: filePaths ) {
            assertTrue( new File(filePath).exists() );
        }

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        assertTrue( dataset.sources.containsKey(imageName) );
        assertTrue( dataset.views.containsKey(imageName) );
        assertTrue( dataset.sources.get(imageName).get().imageData.containsKey(imageDataFormat) );
    }

    @Test
    void addImageBdvN5() throws IOException {
        ImageDataFormat imageDataFormat = ImageDataFormat.BdvN5;
        List<String> filePaths = new ArrayList<>();
        String xmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", imageFormatToFolderName( imageDataFormat ), imageName + ".xml");
        String imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", imageFormatToFolderName( imageDataFormat ), imageName + ".n5");
        filePaths.add(xmlLocation);
        filePaths.add(imageLocation);

        testAddingImageInCertainFormat( imageDataFormat, filePaths );
    }

    @Test
    void addImageBdvOmeZarr() throws IOException {
        ImageDataFormat imageDataFormat = ImageDataFormat.BdvOmeZarr;
        List<String> filePaths = new ArrayList<>();
        String xmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", imageFormatToFolderName( imageDataFormat ), imageName + ".xml");
        String imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", imageFormatToFolderName( imageDataFormat ), imageName + ".ome.zarr");
        filePaths.add(xmlLocation);
        filePaths.add(imageLocation);

        testAddingImageInCertainFormat( imageDataFormat, filePaths );
    }

    @Test
    void addImageOmeZarr() throws IOException {
        ImageDataFormat imageDataFormat = ImageDataFormat.OmeZarr;
        List<String> filePaths = new ArrayList<>();
        String imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", imageFormatToFolderName( imageDataFormat ), imageName + ".ome.zarr");
        filePaths.add(imageLocation);

        testAddingImageInCertainFormat( imageDataFormat, filePaths );
    }

    @Test
    void addBdvFormatImage() {
    }
}