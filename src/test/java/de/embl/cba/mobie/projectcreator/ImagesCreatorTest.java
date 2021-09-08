package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.Dataset;
import de.embl.cba.mobie.serialize.DatasetJsonParser;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.mobie.source.SegmentationSource;
import de.embl.cba.mobie.table.TableDataFormat;
import de.embl.cba.n5.ome.zarr.writers.imgplus.WriteImgPlusToN5BdvOmeZarr;
import de.embl.cba.n5.ome.zarr.writers.imgplus.WriteImgPlusToN5OmeZarr;
import de.embl.cba.n5.util.DownsampleBlock;
import de.embl.cba.n5.util.writers.WriteImgPlusToN5;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
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
    private String imageName;
    private String datasetName;
    private AffineTransform3D sourceTransform;
    private boolean useDefaultSettings;
    private String uiSelectionGroup;
    private String datasetJsonPath;
    private File tempDir;

    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        this.tempDir = tempDir.toFile();
        projectCreator = new ProjectCreator( this.tempDir );
        imagesCreator = projectCreator.getImagesCreator();

        imageName = "testImage";
        datasetName = "test";
        uiSelectionGroup = "testGroup";
        sourceTransform = new AffineTransform3D();
        useDefaultSettings = true;

        projectCreator.getDatasetsCreator().addDataset(datasetName);

        datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "dataset.json" );
    }

    void assertionsForImageAdded( ImageDataFormat imageDataFormat, boolean onlyXmls ) throws IOException {
        assertTrue( new File(datasetJsonPath).exists() );

        List<String> filePaths = new ArrayList<>();
        String xmlLocation = null;
        String imageLocation = null;
        switch( imageDataFormat ) {
            case BdvN5:
                xmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, "images", imageFormatToFolderName(imageDataFormat), imageName + ".xml");
                imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, "images", imageFormatToFolderName(imageDataFormat), imageName + ".n5");
                break;

            case BdvOmeZarr:
                xmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, "images", imageFormatToFolderName(imageDataFormat), imageName + ".xml");
                imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, "images", imageFormatToFolderName(imageDataFormat), imageName + ".ome.zarr");
                break;

            case OmeZarr:
                imageLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, "images", imageFormatToFolderName(imageDataFormat), imageName + ".ome.zarr");
                break;
        }

        if ( xmlLocation != null ) {
            filePaths.add( xmlLocation );
        }

        if ( !onlyXmls ) {
            filePaths.add(imageLocation);
        }

        for ( String filePath: filePaths ) {
            assertTrue( new File(filePath).exists() );
        }

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        assertTrue( dataset.sources.containsKey(imageName) );
        assertTrue( dataset.views.containsKey(imageName) );
        assertTrue( dataset.sources.get(imageName).get().imageData.containsKey(imageDataFormat) );
    }

    void assertionsForTableAdded( ImageDataFormat imageDataFormat ) throws IOException {
        String tablePath = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(), datasetName, "tables", imageName, "default.tsv" );
        assertTrue( new File(tablePath).exists() );

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        SegmentationSource segmentationSource = ((SegmentationSource) dataset.sources.get(imageName).get());
        assertTrue( segmentationSource.tableData.containsKey(TableDataFormat.TabDelimitedFile) );
    }

    ImagePlus makeImage() {
        // make an image with random values, same size as the imagej sample head image
        return IJ.createImage(imageName, "8-bit noise", 186, 226, 27);
    }

    ImagePlus makeSegmentation() {
        // make an image with 3 boxes with pixel values 1, 2 and 3 as mock segmentation. Same size as imagej sample
        // head image
        int width = 186;
        int height = 226;
        int depth = 27;

        ImagePlus seg = IJ.createImage(imageName, "8-bit black", width, height, depth);
        for ( int i = 1; i<depth; i++ ) {
            ImageProcessor ip = seg.getImageStack().getProcessor(i);
            ip.setValue(1);
            ip.setRoi(5, 5, 67, 25);
            ip.fill();

            ip.setValue(2);
            ip.setRoi(51, 99, 67, 25);
            ip.fill();

            ip.setValue(3);
            ip.setRoi(110, 160, 67, 25);
            ip.fill();
        }

        return seg;
    }

    String writeImageAndGetPath( ImageDataFormat imageDataFormat ) {
        // save example image for testing adding bdv format images
        ImagePlus imp = makeImage();
        DownsampleBlock.DownsamplingMethod downsamplingMethod = DownsampleBlock.DownsamplingMethod.Average;
        Compression compression = new GzipCompression();
        String filePath;

        // gzip compression by default
        switch( imageDataFormat ) {
            case BdvN5:
                filePath = new File(tempDir, imageName + ".xml").getAbsolutePath();
                new WriteImgPlusToN5().export(imp, filePath, sourceTransform, downsamplingMethod,
                        compression, new String[]{imageName} );
                break;

            case BdvOmeZarr:
                filePath = new File(tempDir, imageName + ".xml").getAbsolutePath();
                new WriteImgPlusToN5BdvOmeZarr().export(imp, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName} );
                break;

            case OmeZarr:
                filePath = new File(tempDir, imageName + ".ome.zarr").getAbsolutePath();
                new WriteImgPlusToN5OmeZarr().export(imp, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            default:
                throw new UnsupportedOperationException();

        }

        return filePath;
    }

    void testAddingImageInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException {

        // make an image with random values, same size as the imagej sample head image
        ImagePlus imp = makeImage();

        imagesCreator.addImage( imp, imageName, datasetName,
                imageDataFormat, ProjectCreator.ImageType.image,
                sourceTransform, useDefaultSettings, uiSelectionGroup );

        assertionsForImageAdded( imageDataFormat, false );
    }

    void testAddingSegmentationInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException {
        ImagePlus seg = makeSegmentation();

        imagesCreator.addImage( seg, imageName, datasetName,
                imageDataFormat, ProjectCreator.ImageType.segmentation,
                sourceTransform, useDefaultSettings, uiSelectionGroup );

        assertionsForImageAdded( imageDataFormat, false );
        assertionsForTableAdded( imageDataFormat );
    }

    void testLinkingImagesInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException, SpimDataException {

        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat );

        imagesCreator.addBdvFormatImage( new File(filePath), datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.link, uiSelectionGroup, imageDataFormat );

        assertionsForImageAdded( imageDataFormat, true );
    }

    void testCopyingImagesInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException, SpimDataException {

        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat );

        imagesCreator.addBdvFormatImage( new File(filePath), datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.copy, uiSelectionGroup, imageDataFormat );

        assertionsForImageAdded( imageDataFormat, false );
    }

    void testMovingImagesInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException, SpimDataException {

        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat );

        imagesCreator.addBdvFormatImage( new File(filePath), datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.move, uiSelectionGroup, imageDataFormat );

        assertionsForImageAdded( imageDataFormat, false );
    }

    @Test
    void addImageBdvN5() throws IOException {
        testAddingImageInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void addSegmentationBdvN5() throws IOException {
        testAddingSegmentationInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void addImageBdvOmeZarr() throws IOException {
        testAddingImageInCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void addSegmentationBdvOmeZarr() throws IOException {
        testAddingSegmentationInCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void addImageOmeZarr() throws IOException {
        testAddingImageInCertainFormat( ImageDataFormat.OmeZarr );
    }

    @Test
    void addSegmentationOmeZarr() throws IOException {
        testAddingSegmentationInCertainFormat( ImageDataFormat.OmeZarr );
    }

    @Test
    void linkToImageBdvN5() throws IOException, SpimDataException {
        testLinkingImagesInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void linkToImageBdvOmeZarr() throws IOException, SpimDataException {
        testLinkingImagesInCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void copyImageBdvN5() throws IOException, SpimDataException {
        testCopyingImagesInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void copyImageBdvOmeZarr() throws IOException, SpimDataException {
        testCopyingImagesInCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void copyImageOmeZarr() throws IOException, SpimDataException {
        testCopyingImagesInCertainFormat( ImageDataFormat.OmeZarr );
    }

    @Test
    void moveImageBdvN5() throws IOException, SpimDataException {
        testMovingImagesInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void moveImageBdvOmeZarr() throws IOException, SpimDataException {
        testMovingImagesInCertainFormat( ImageDataFormat.BdvOmeZarr );
    }

    @Test
    void moveImageOmeZarr() throws IOException, SpimDataException {
        testMovingImagesInCertainFormat( ImageDataFormat.OmeZarr );
    }
}