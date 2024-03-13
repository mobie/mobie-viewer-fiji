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
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.table.TableDataFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.embl.mobie.lib.create.JSONValidator.validate;
import static org.embl.mobie.lib.create.ProjectCreatorTestHelper.createImage;
import static org.embl.mobie.lib.create.ProjectCreatorTestHelper.createLabels;
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

        datasetJsonPath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "dataset.json" );
    }

    void assertionsForImageData( ImageData< ? > imageData ) {
        // check setup name is equal to image name
        assertEquals( imageData.getSourcePair( 0 ).getB().getName(), imageName );
    }

    void assertionsForDataset( ) throws IOException {
        assertTrue( new File(datasetJsonPath).exists() );
        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        assertTrue( dataset.sources().containsKey(imageName) );
        assertTrue( dataset.views().containsKey(imageName) );
        assertTrue( ((ImageDataSource)dataset.sources().get(imageName)).imageData.containsKey( ImageDataFormat.OmeZarr) );
        // Check that this follows JSON schema
        assertTrue( validate( datasetJsonPath, JSONValidator.datasetSchemaURL ) );
    }

    void assertionsForOmeZarr()
    {
        String imageLocation = IOHelper.combinePath(
                projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName,
                "images",
                imageName + ".ome.zarr");

        assertTrue( new File(imageLocation).exists() );

        assertionsForImageData( ImageDataOpener.open( imageLocation ) );
    }

    void assertionsForImageAdded() throws IOException {
        assertionsForDataset();
        assertionsForOmeZarr();
    }

    void assertionsForTableAdded( ) throws IOException {
        String tablePath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(), datasetName, "tables", imageName, "default.tsv" );
        assertTrue( new File(tablePath).exists() );

        Dataset dataset = new DatasetJsonParser().parseDataset(datasetJsonPath);
        SegmentationDataSource segmentationData = (( SegmentationDataSource ) dataset.sources().get(imageName));
        assertTrue( segmentationData.tableData.containsKey(TableDataFormat.TSV ) );
    }

    String writeImageAndGetPath( boolean is2D ) {
        // add example image
        ImagePlus imp = createImage( imageName, is2D );
        String filePath = new File(tempDir, imageName + ".ome.zarr").getAbsolutePath();
        OMEZarrWriter.write( imp, filePath, OMEZarrWriter.ImageType.Intensities, true );
        return filePath;
    }

    void addImage( boolean is2D ) {
        ImagePlus imp = createImage( imageName, is2D );
        imagesCreator.addImage( imp, imageName, datasetName,
                ProjectCreator.ImageType.Image, sourceTransform,
                uiSelectionGroup, false );
    }

    void testAddingImage( boolean is2D ) throws IOException {
        addImage( is2D );
        assertionsForImageAdded( );
    }

    void testAddingSegmentation() throws IOException{
        ImagePlus seg = createLabels( imageName );

        imagesCreator.addImage( seg, imageName, datasetName,
                ProjectCreator.ImageType.Segmentation,
                sourceTransform, uiSelectionGroup, false );

        assertionsForImageAdded();
        assertionsForTableAdded();
    }

    // FIXME: Do we need all the is2D booleans here?
    //        Ask Kimberly in an issue

    void testLinkingImages( boolean is2D ) throws IOException {

        // save example image
        String filePath = writeImageAndGetPath( is2D );

        imagesCreator.addOMEZarrImage( filePath, imageName, datasetName,
                ProjectCreator.ImageType.Image, ProjectCreator.AddMethod.Link,
                uiSelectionGroup, false );

        assertionsForImageAdded();
    }

    void copyImage(  boolean is2D ) {
        // save example image
        String filePath = writeImageAndGetPath( is2D );

        imagesCreator.addOMEZarrImage( filePath, imageName, datasetName,
                ProjectCreator.ImageType.Image,
                ProjectCreator.AddMethod.Copy, uiSelectionGroup, false );
    }

    void testCopyingImages( boolean is2D ) throws IOException {
        copyImage( is2D );
        assertionsForImageAdded();
    }

    @Test
    void addImageOmeZarr() throws IOException {
        testAddingImage( false );
    }

    @Test
    void addSegmentationOmeZarr() throws IOException {
        testAddingSegmentation();
    }

    @Test
    void linkOMEZarrImage() throws IOException {
        testLinkingImages( false );
    }

    @Test
    void copyOMEZarrImage() throws IOException {
        testCopyingImages( false );
    }

    @Test
    void add2DImageTo3DDataset() throws IOException {
        testAddingImage( true );
    }

    @Test
    void copy2DImageTo3DDataset() throws IOException {
        testCopyingImages( true );
    }

    @Test
    void add3DImageTo2DDataset() {
        projectCreator.getDatasetsCreator().makeDataset2D(datasetName, true);
        assertThrows( UnsupportedOperationException.class, () -> {
            addImage( false);
        } );
    }

    @Test
    void copy3DImageTo2DDataset() {
        projectCreator.getDatasetsCreator().makeDataset2D(datasetName, true);
        assertThrows( UnsupportedOperationException.class, () -> {
            copyImage( false);
        } );
    }
}
