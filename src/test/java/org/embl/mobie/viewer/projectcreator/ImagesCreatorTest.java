package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.n5.writers.WriteImgPlusToN5;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5OmeZarr;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.source.SegmentationSource;
import org.embl.mobie.viewer.table.TableDataFormat;

import de.embl.cba.tables.FileAndUrlUtils;
import ij.ImagePlus;
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

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorTestHelper.makeImage;
import static org.embl.mobie.viewer.projectcreator.ProjectCreatorTestHelper.makeSegmentation;
import static org.junit.jupiter.api.Assertions.*;

class ImagesCreatorTest {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;
    private ImagesCreator imagesCreator;
    private String imageName;
    private String datasetName;
    private AffineTransform3D sourceTransform;
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

        projectCreator.getDatasetsCreator().addDataset(datasetName, false);

        datasetJsonPath = FileAndUrlUtils.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "dataset.json" );
    }

    void assertionsForImageAdded( ImageDataFormat imageDataFormat, boolean onlyXmls ) throws IOException {
        assertTrue( new File(datasetJsonPath).exists() );

        List<String> filePaths = new ArrayList<>();
        String xmlLocation = null;
        String imageLocation = null;
        switch( imageDataFormat ) {
            case BdvN5:
                xmlLocation = FileAndUrlUtils.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                        datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat), imageName + ".xml");
                imageLocation = FileAndUrlUtils.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                        datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat), imageName + ".n5");
                break;

            case OmeZarr:
                imageLocation = FileAndUrlUtils.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                        datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat), imageName + ".ome.zarr");
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

    void assertionsForTableAdded( ) throws IOException {
        String tablePath = FileAndUrlUtils.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "tables", imageName, "default.tsv" );
        assertTrue( new File(tablePath).exists() );

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        SegmentationSource segmentationSource = ((SegmentationSource) dataset.sources.get(imageName).get());
        assertTrue( segmentationSource.tableData.containsKey(TableDataFormat.TabDelimitedFile) );
    }

    String writeImageAndGetPath( ImageDataFormat imageDataFormat, boolean is2D ) {
        // save example image for testing adding bdv format images
        ImagePlus imp = makeImage( imageName, is2D );
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

    void addImageInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws SpimDataException, IOException {
        // make an image with random values, same size as the imagej sample head image
        ImagePlus imp = makeImage( imageName, is2D );

        imagesCreator.addImage( imp, imageName, datasetName,
                imageDataFormat, ProjectCreator.ImageType.image,
                sourceTransform, uiSelectionGroup, false );
    }

    void testAddingImageInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws IOException, SpimDataException {
        addImageInCertainFormat( imageDataFormat, is2D );
        assertionsForImageAdded( imageDataFormat, false );
    }

    void testAddingSegmentationInCertainFormat( ImageDataFormat imageDataFormat ) throws IOException, SpimDataException {
        ImagePlus seg = makeSegmentation( imageName );

        imagesCreator.addImage( seg, imageName, datasetName,
                imageDataFormat, ProjectCreator.ImageType.segmentation,
                sourceTransform, uiSelectionGroup, false );

        assertionsForImageAdded( imageDataFormat, false );
        assertionsForTableAdded();
    }

    void testLinkingImagesInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws IOException, SpimDataException {

        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat, is2D );

        imagesCreator.addBdvFormatImage( new File(filePath), imageName, datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.link, uiSelectionGroup, imageDataFormat, false );

        assertionsForImageAdded( imageDataFormat, true );
    }

    void copyImageInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws SpimDataException, IOException {
        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat, is2D );

        imagesCreator.addBdvFormatImage( new File(filePath), imageName, datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.copy, uiSelectionGroup, imageDataFormat, false );
    }

    void testCopyingImagesInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws IOException, SpimDataException {
        copyImageInCertainFormat( imageDataFormat, is2D );
        assertionsForImageAdded( imageDataFormat, false );
    }

    void testMovingImagesInCertainFormat( ImageDataFormat imageDataFormat, boolean is2D ) throws IOException, SpimDataException {

        // save example image
        String filePath = writeImageAndGetPath( imageDataFormat, is2D );

        imagesCreator.addBdvFormatImage( new File(filePath), imageName, datasetName, ProjectCreator.ImageType.image,
                ProjectCreator.AddMethod.move, uiSelectionGroup, imageDataFormat, false );

        assertionsForImageAdded( imageDataFormat, false );
    }

    @Test
    void addImageBdvN5() throws IOException, SpimDataException {
        testAddingImageInCertainFormat( ImageDataFormat.BdvN5, false );
    }

    @Test
    void addSegmentationBdvN5() throws IOException, SpimDataException {
        testAddingSegmentationInCertainFormat( ImageDataFormat.BdvN5 );
    }

    @Test
    void addImageOmeZarr() throws IOException, SpimDataException {
        testAddingImageInCertainFormat( ImageDataFormat.OmeZarr, false );
    }

    @Test
    void addSegmentationOmeZarr() throws IOException, SpimDataException {
        testAddingSegmentationInCertainFormat( ImageDataFormat.OmeZarr );
    }

    @Test
    void linkToImageBdvN5() throws IOException, SpimDataException {
        testLinkingImagesInCertainFormat( ImageDataFormat.BdvN5, false );
    }

    @Test
    void copyImageBdvN5() throws IOException, SpimDataException {
        testCopyingImagesInCertainFormat( ImageDataFormat.BdvN5, false );
    }

    @Test
    void copyImageOmeZarr() throws IOException, SpimDataException {
        testCopyingImagesInCertainFormat( ImageDataFormat.OmeZarr, false );
    }

    @Test
    void moveImageBdvN5() throws IOException, SpimDataException {
        testMovingImagesInCertainFormat( ImageDataFormat.BdvN5, false );
    }

    @Test
    void moveImageOmeZarr() throws IOException, SpimDataException {
        testMovingImagesInCertainFormat( ImageDataFormat.OmeZarr, false );
    }

    @Test
    void add2DImageTo3DDataset() throws SpimDataException, IOException {
        testAddingImageInCertainFormat( ImageDataFormat.BdvN5, true );
    }

    @Test
    void copy2DImageTo3DDataset() throws SpimDataException, IOException {
        testCopyingImagesInCertainFormat( ImageDataFormat.BdvN5, true );
    }

    @Test
    void add3DImageTo2DDataset() {
        projectCreator.getDatasetsCreator().makeDataset2D(datasetName, true);
        assertThrows( UnsupportedOperationException.class, () -> {
            addImageInCertainFormat( ImageDataFormat.BdvN5, false);
        } );
    }

    @Test
    void copy3DImageTo2DDataset() {
        projectCreator.getDatasetsCreator().makeDataset2D(datasetName, true);
        assertThrows( UnsupportedOperationException.class, () -> {
            copyImageInCertainFormat( ImageDataFormat.BdvN5, false);
        } );
    }
}
