package org.embl.mobie.command.open;

import org.embl.mobie.lib.data.GridImagesAndLabelsDataSetter;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.transform.GridType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the {@code OpenImageAndLabelsCommand} data path.
 *
 * Bypasses the UI by driving {@link GridImagesAndLabelsDataSetter} directly —
 * the same class the production {@code MoBIE(List<String> imagePaths, ...)}
 * constructor now uses to populate its dataset, minus
 * {@code initUiAndShowView(...)}.
 *
 * The UI-driving variants are kept in
 * {@code Open2DTIFFImageAndIlastikSegmentationCommandTest} and friends for
 * manual visual verification.
 */
public class OpenImageAndLabelsCommandHeadlessTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void imageWithLabelsAndLabelTable()
    {
        // Same inputs as Open2DTIFFImageAndIlastikSegmentationCommandTest.
        String imagePath = new File( "src/test/resources/ilastik-2d/image.tif" ).getAbsolutePath();
        String labelsPath = new File( "src/test/resources/ilastik-2d/labels.h5" ).getAbsolutePath();
        String tablePath = new File( "src/test/resources/ilastik-2d/table.csv" ).getAbsolutePath();

        Dataset dataset = new Dataset( "test" );
        new GridImagesAndLabelsDataSetter(
                Collections.singletonList( imagePath ),
                Collections.singletonList( labelsPath ),
                Collections.singletonList( tablePath ),
                null,
                GridType.Transformed
        ).addToDataset( dataset );

        // Two data sources: image + labels (names derive from filenames).
        assertEquals( 2, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "image" ) );
        SegmentationDataSource labels =
                ( SegmentationDataSource ) dataset.sources().get( "labels" );
        assertNotNull( labels );
        assertNotNull( labels.getTableData() );
        assertTrue( ! labels.getTableData().isEmpty(),
                "labels source should carry the linked CSV table" );

        // One grid view called "all images" gets created.
        View view = dataset.views().get( "all images" );
        assertNotNull( view, "expected the grid view 'all images'" );

        // The view should contain exactly one ImageDisplay, one SegmentationDisplay
        // and one RegionDisplay.
        long imageDisplays = view.displays().stream()
                .filter( d -> d instanceof ImageDisplay ).count();
        long segDisplays = view.displays().stream()
                .filter( d -> d instanceof SegmentationDisplay ).count();
        long regionDisplays = view.displays().stream()
                .filter( d -> d instanceof RegionDisplay ).count();
        assertEquals( 1, imageDisplays );
        assertEquals( 1, segDisplays );
        assertEquals( 1, regionDisplays );

        ImageDisplay< ? > imageDisplay = ( ImageDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof ImageDisplay )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay" ) );
        assertEquals( Collections.singletonList( "image" ), imageDisplay.getSources() );

        SegmentationDisplay< ? > segDisplay = ( SegmentationDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof SegmentationDisplay )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no SegmentationDisplay" ) );
        assertEquals( Collections.singletonList( "labels" ), segDisplay.getSources() );
        assertTrue( segDisplay.showTable(),
                "segmentation display should show its label table" );
    }

    @Test
    public void imageOnlyWithoutLabels()
    {
        // Mirrors OpenIlastik2ChannelImageCommandTest.test() — just an image
        // (no labels, no labels table). The data setter should still produce
        // a populated view with one ImageDisplay + a RegionDisplay.
        String imagePath = new File( "src/test/resources/ilastik-2d/image.tif" ).getAbsolutePath();

        Dataset dataset = new Dataset( "test" );
        new GridImagesAndLabelsDataSetter(
                Collections.singletonList( imagePath ),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                GridType.Transformed
        ).addToDataset( dataset );

        assertEquals( 1, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "image" ) );

        View view = dataset.views().get( "all images" );
        assertNotNull( view );

        // One ImageDisplay + one RegionDisplay, no SegmentationDisplay.
        assertEquals( 1, view.displays().stream()
                .filter( d -> d instanceof ImageDisplay ).count() );
        assertEquals( 0, view.displays().stream()
                .filter( d -> d instanceof SegmentationDisplay ).count() );
        assertEquals( 1, view.displays().stream()
                .filter( d -> d instanceof RegionDisplay ).count() );
    }
}
