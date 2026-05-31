package org.embl.mobie.command.open;

import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.CollectionDataSetter;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.SpotDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.GridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for collection-table opening logic.
 *
 * These tests bypass the BDV/Swing UI by driving {@link CollectionDataSetter}
 * directly (the same code path that {@code OpenCollectionTableCommand} runs
 * after reading the table). They assert on the resulting {@link Dataset}
 * (sources, displays, views, transformations) instead of opening BDV windows.
 *
 * The UI-driving variants of these tests are kept in
 * {@code OpenCollectionTableCommandTest} for manual visual verification.
 */
public class OpenCollectionTableCommandHeadlessTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private static Dataset buildDataset( String tableFileRelativeToProject )
    {
        String tableUri = new File( tableFileRelativeToProject ).getAbsolutePath();
        String rootPath = IOHelper.getParentLocation( tableUri );

        Table table = org.embl.mobie.lib.table.saw.TableOpener.open( tableUri );

        Dataset dataset = new Dataset( "test" );
        new CollectionDataSetter( table, rootPath ).addTableToDataset( dataset );
        return dataset;
    }

    @Test
    public void singleBlobs()
    {
        // blobs-collection.txt has a single row with only the "uri" column set.
        // Verifies defaults: the source name is derived from the file name,
        // a default view and ImageDisplay are created.
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-collection.txt" );

        assertEquals( 1, dataset.sources().size(), "expected exactly one source" );
        DataSource source = dataset.sources().get( "blobs" );
        assertInstanceOf( ImageDataSource.class, source, "source should be an intensities image" );

        assertEquals( 1, dataset.views().size(), "expected exactly one view" );
        View view = dataset.views().get( "blobs" );
        assertNotNull( view, "view named after the source should exist" );

        List< Display< ? > > displays = view.displays();
        assertEquals( 1, displays.size(), "single-source view should have one display" );
        assertInstanceOf( ImageDisplay.class, displays.get( 0 ) );
        assertEquals( Collections.singletonList( "blobs" ), displays.get( 0 ).getSources() );
    }

    @Test
    public void segmentedBlobs()
    {
        // blobs-table.txt covers intensities, labels with a labels_table, an
        // exclusive view, an affine transformation, contrast limits and
        // multiple named views.
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-table.txt" );

        // 3 sources: BLOBS (intensities), blobs-labels (labels), mri-stack (intensities)
        assertEquals( 3, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "BLOBS" ) );
        assertInstanceOf( SegmentationDataSource.class, dataset.sources().get( "blobs-labels" ) );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "mri-stack" ) );

        // The labels source carries the labels_table reference.
        SegmentationDataSource labels = ( SegmentationDataSource ) dataset.sources().get( "blobs-labels" );
        assertNotNull( labels.getTableData(), "labels source must carry table data" );
        assertTrue( ! labels.getTableData().isEmpty() );

        // Two views: "segmented blobs" (BLOBS + blobs-labels) and "mri" (mri-stack, exclusive).
        Map< String, View > views = dataset.views();
        assertEquals( 2, views.size() );
        View segmented = views.get( "segmented blobs" );
        View mri = views.get( "mri" );
        assertNotNull( segmented );
        assertNotNull( mri );

        // "mri" was marked exclusive in the table.
        assertTrue( mri.isExclusive(), "mri view should be exclusive" );
        assertEquals( Boolean.FALSE, segmented.isExclusive() );

        // The BLOBS image has the affine transformation attached.
        boolean blobsHasAffine = segmented.transformations().stream()
                .anyMatch( t -> t instanceof AffineTransformation
                        && t.getSources().contains( "BLOBS" ) );
        assertTrue( blobsHasAffine, "BLOBS should have an affine transformation" );

        // Contrast limits from the table (20;200) must be on the BLOBS display.
        ImageDisplay< ? > blobsDisplay = ( ImageDisplay< ? > ) segmented.displays().stream()
                .filter( d -> d instanceof ImageDisplay && d.getSources().contains( "BLOBS" ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay for BLOBS" ) );
        double[] limits = blobsDisplay.getContrastLimits( "BLOBS" );
        assertNotNull( limits );
        assertEquals( 2, limits.length );
        assertEquals( 20.0, limits[ 0 ], 1e-9 );
        assertEquals( 200.0, limits[ 1 ], 1e-9 );

        // mri view uses both "mri1" and "mri2" ui selection groups.
        assertTrue( mri.getUiSelectionGroups().contains( "mri1" ) );
        assertTrue( mri.getUiSelectionGroups().contains( "mri2" ) );
    }

    @Test
    public void blobsGrid()
    {
        // blobs-grid-collection.txt: two rows that share the same view ("blobs"),
        // the same display name ("image") and the same grid ("grid").
        // Both sources should land in one ImageDisplay; a GridTransformation
        // and a region display should be generated.
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-grid-collection.txt" );

        assertEquals( 2, dataset.sources().size() );
        assertNotNull( dataset.sources().get( "blobs1" ) );
        assertNotNull( dataset.sources().get( "blobs2" ) );

        View view = dataset.views().get( "blobs" );
        assertNotNull( view );

        // One ImageDisplay called "image" carrying both sources, plus the
        // auto-generated region display for the grid.
        ImageDisplay< ? > imageDisplay = ( ImageDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof ImageDisplay )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay in view" ) );
        assertEquals( "image", imageDisplay.getName() );
        assertEquals( 2, imageDisplay.getSources().size() );
        assertTrue( imageDisplay.getSources().containsAll( Arrays.asList( "blobs1", "blobs2" ) ) );

        // Per-source contrast limits must match the table.
        assertEquals( 0.0,   imageDisplay.getContrastLimits( "blobs1" )[ 0 ], 1e-9 );
        assertEquals( 255.0, imageDisplay.getContrastLimits( "blobs1" )[ 1 ], 1e-9 );
        assertEquals( 50.0,  imageDisplay.getContrastLimits( "blobs2" )[ 0 ], 1e-9 );
        assertEquals( 200.0, imageDisplay.getContrastLimits( "blobs2" )[ 1 ], 1e-9 );

        long regionDisplays = view.displays().stream()
                .filter( d -> d instanceof RegionDisplay ).count();
        assertEquals( 1, regionDisplays, "grid should produce a region display" );

        long grids = view.transformations().stream()
                .filter( t -> t instanceof GridTransformation ).count();
        assertEquals( 1, grids, "grid should produce exactly one GridTransformation" );
    }

    @Test
    public void spots2D()
    {
        // spots-2d-collection.txt has a single spots row.
        Dataset dataset = buildDataset( "src/test/resources/collections/spots-2d-collection.txt" );

        assertEquals( 1, dataset.sources().size() );
        DataSource source = dataset.sources().get( "spots" );
        assertInstanceOf( SpotDataSource.class, source );

        View view = dataset.views().get( "spots" );
        assertNotNull( view );

        SpotDisplay< ? > spotDisplay = ( SpotDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof SpotDisplay )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no SpotDisplay in view" ) );
        assertEquals( Collections.singletonList( "spots" ), spotDisplay.getSources() );
    }

    @Test
    public void autoContrastBlobs()
    {
        // blobs-auto-contrast-collection.csv: two rows in a grid with
        // contrast_limits=auto. With no explicit display column the display
        // name falls back to the grid name ("grid"), so both sources end up
        // in the same ImageDisplay, each with the auto-contrast sentinel
        // (a length-1 array of {0.0}, see CollectionDataSetter#getContrastLimits).
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-auto-contrast-collection.csv" );

        assertEquals( 2, dataset.sources().size() );
        assertNotNull( dataset.sources().get( "blobs-uint16" ) );
        assertNotNull( dataset.sources().get( "blobs-uint16-brighter" ) );

        // With no "display" column but a "grid" column, the display name (and
        // therefore the view name) falls back to the grid column ("grid").
        // See CollectionDataSetter#getDisplayName / #getViewName.
        View view = dataset.views().get( "grid" );
        assertNotNull( view, "view should be named after the grid column" );

        ImageDisplay< ? > imageDisplay = ( ImageDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof ImageDisplay )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay in view" ) );
        assertEquals( 2, imageDisplay.getSources().size(),
                "both grid sources should share one ImageDisplay" );

        for ( String source : imageDisplay.getSources() )
        {
            double[] limits = imageDisplay.getContrastLimits( source );
            assertNotNull( limits, "auto-contrast must still be encoded as an array" );
            assertEquals( 1, limits.length, "auto-contrast is encoded as a length-1 array" );
            assertEquals( 0.0, limits[ 0 ], 1e-9 );
        }

        // The grid produces a region display and a single GridTransformation.
        assertEquals( 1, view.displays().stream()
                .filter( d -> d instanceof RegionDisplay ).count() );
        assertEquals( 1, view.transformations().stream()
                .filter( t -> t instanceof GridTransformation ).count() );
    }
}
