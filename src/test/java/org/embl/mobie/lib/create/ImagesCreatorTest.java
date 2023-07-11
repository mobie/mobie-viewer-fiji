/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.n5.writers.WriteImagePlusToN5;
import org.embl.mobie.io.ome.zarr.writers.imageplus.WriteImagePlusToN5OmeZarr;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.table.TableDataFormat;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.embl.mobie.lib.create.JSONValidator.validate;
import static org.embl.mobie.lib.create.ProjectCreatorTestHelper.makeImage;
import static org.embl.mobie.lib.create.ProjectCreatorTestHelper.makeSegmentation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        datasetJsonPath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "dataset.json" );
    }

    void assertionsForSpimData( SpimData spimData ) {
        // check setup name is equal to image name
        assertEquals( spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getName(), imageName );
    }

    void assertionsForDataset( ImageDataFormat imageDataFormat ) throws IOException {
        assertTrue( new File(datasetJsonPath).exists() );
        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        assertTrue( dataset.sources().containsKey(imageName) );
        assertTrue( dataset.views().containsKey(imageName) );
        assertTrue( (( ImageDataSource )dataset.sources().get(imageName)).imageData.containsKey(imageDataFormat) );
        // Check that this follows JSON schema
        assertTrue( validate( datasetJsonPath, JSONValidator.datasetSchemaURL ) );
    }

    void assertionsForN5( boolean onlyXmls ) throws SpimDataException {
        ImageDataFormat imageDataFormat = ImageDataFormat.BdvN5;

        String xmlLocation = IOHelper.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat),
                imageName + ".xml");
        String imageLocation = IOHelper.combinePath(projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat),
                imageName + ".n5");

        assertTrue( new File(xmlLocation).exists() );

        if ( !onlyXmls ) {
            assertTrue( new File(imageLocation).exists() );
        }

        SpimData spimData = (SpimData) new SpimDataOpener().open( xmlLocation, imageDataFormat );
        assertionsForSpimData( spimData );
    }

    void assertionsForOmeZarr() throws SpimDataException {
        ImageDataFormat imageDataFormat = ImageDataFormat.OmeZarr;

        String imageLocation = IOHelper.combinePath(
                projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "images",
                ProjectCreatorHelper.imageFormatToFolderName(imageDataFormat), imageName + ".ome.zarr");

        assertTrue( new File(imageLocation).exists() );

        SpimData spimData = (SpimData) new SpimDataOpener().open( imageLocation, imageDataFormat );
        assertionsForSpimData( spimData );
    }

    void assertionsForImageAdded( ImageDataFormat imageDataFormat, boolean onlyXmls ) throws IOException, SpimDataException {
        assertionsForDataset( imageDataFormat );

        switch( imageDataFormat ) {
            case BdvN5:
                assertionsForN5( onlyXmls );
                break;

            case OmeZarr:
                assertionsForOmeZarr();
                break;
        }
    }

    void assertionsForTableAdded( ) throws IOException {
        String tablePath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "tables", imageName, "default.tsv" );
        assertTrue( new File(tablePath).exists() );

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        SegmentationDataSource segmentationData = (( SegmentationDataSource ) dataset.sources().get(imageName));
        assertTrue( segmentationData.tableData.containsKey(TableDataFormat.TSV ) );
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
                new WriteImagePlusToN5().export(imp, filePath, sourceTransform, downsamplingMethod,
                        compression, new String[]{imageName} );
                break;

            case OmeZarr:
                filePath = new File(tempDir, imageName + ".ome.zarr").getAbsolutePath();
                new WriteImagePlusToN5OmeZarr().export(imp, filePath, sourceTransform,
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
