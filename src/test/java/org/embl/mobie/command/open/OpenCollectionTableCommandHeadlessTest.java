package org.embl.mobie.command.open;

import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
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
import org.embl.mobie.lib.serialize.transformation.ThinPlateSplineTransformation;
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

    @Test
    public void excelSheet()
    {
        // The .xlsx table goes through the EXCEL TableDataFormat code path.
        // We just assert it parses without error and produces sources.
        Dataset dataset = buildDataset( "src/test/resources/collections/clem-collection.xlsx" );
        assertTrue( dataset.sources().size() > 0, "xlsx collection should yield at least one source" );
        assertTrue( dataset.views().size() > 0, "xlsx collection should yield at least one view" );
    }

    @Test
    public void alphaBlendingOrder()
    {
        // alpha-blend-collection.csv has blend=alpha on one row and blank on
        // the other. The blend column should map to BlendingMode.Alpha for the
        // first row and stay null otherwise.
        Dataset dataset = buildDataset( "src/test/resources/collections/alpha-blend-collection.csv" );

        View view = dataset.views().get( "all" );
        assertNotNull( view );

        ImageDisplay< ? > alphaDisplay = ( ImageDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof ImageDisplay && d.getSources().contains( "b" ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay for source 'b'" ) );
        assertEquals( BlendingMode.Alpha, alphaDisplay.getBlendingMode() );

        ImageDisplay< ? > nonAlphaDisplay = ( ImageDisplay< ? > ) view.displays().stream()
                .filter( d -> d instanceof ImageDisplay && d.getSources().contains( "a" ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no ImageDisplay for source 'a'" ) );
        // CollectionDataSetter#getBlendingMode returns null when blend is not
        // "alpha"; the display then falls back to its default (Sum).
        assertEquals( BlendingMode.Sum, nonAlphaDisplay.getBlendingMode() );
    }

    @Test
    public void addImageTwice()
    {
        // blobs-image-twice-collection.csv references blobs.tif three times
        // with the same view "blobs1" twice + once view "blobs2".
        // Duplicate source names get a " (n)" suffix (see
        // CollectionDataSetter#getDataName), so all three rows produce
        // distinct DataSource entries.
        // https://github.com/mobie/mobie-viewer-fiji/issues/1244
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-image-twice-collection.csv" );

        assertEquals( 3, dataset.sources().size(),
                "duplicate URIs should still create distinct sources via name disambiguation" );
        assertNotNull( dataset.sources().get( "blobs" ) );
        assertNotNull( dataset.sources().get( "blobs (1)" ) );
        assertNotNull( dataset.sources().get( "blobs (2)" ) );
    }

    @Test
    public void blobsAndMri()
    {
        // blobs-mri-collection.csv has two intensities sources in the same
        // view "all", each with its own affine — covers an "uppercase Uri"
        // header (column name matching is case-insensitive).
        Dataset dataset = buildDataset( "src/test/resources/collections/blobs-mri-collection.csv" );

        assertEquals( 2, dataset.sources().size() );
        assertNotNull( dataset.sources().get( "blobs" ) );
        assertNotNull( dataset.sources().get( "mri-stack" ) );

        View view = dataset.views().get( "all" );
        assertNotNull( view );

        long affines = view.transformations().stream()
                .filter( t -> t instanceof AffineTransformation ).count();
        assertEquals( 2, affines, "each source should contribute one affine transformation" );
    }

    @Test
    public void spots3dAffine()
    {
        // spots-3d-affine-collection.txt: a single spots source with an affine.
        // Verifies that the affine code path also fires for spots (not only images).
        Dataset dataset = buildDataset( "src/test/resources/collections/spots-3d-affine-collection.txt" );

        assertEquals( 1, dataset.sources().size() );
        assertInstanceOf( SpotDataSource.class, dataset.sources().get( "spots" ) );

        View view = dataset.views().get( "spots" );
        assertNotNull( view );

        boolean spotsHasAffine = view.transformations().stream()
                .anyMatch( t -> t instanceof AffineTransformation
                        && t.getSources().contains( "spots" ) );
        assertTrue( spotsHasAffine, "spots source should have an affine transformation" );
    }

    @Test
    public void thinPlateSplineMri()
    {
        // thinplatespline-mri-collection.tsv carries a thin_plate_spline JSON
        // payload — exercises the TPS branch in
        // CollectionDataSetter#getTransformations.
        Dataset dataset = buildDataset( "src/test/resources/collections/thinplatespline-mri-collection.tsv" );

        View view = dataset.views().get( "mri-tps-transformed" );
        assertNotNull( view );

        ThinPlateSplineTransformation tps = ( ThinPlateSplineTransformation ) view.transformations().stream()
                .filter( t -> t instanceof ThinPlateSplineTransformation )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no TPS transformation found" ) );
        assertTrue( tps.getSources().contains( "mri-tps-transformed" ) );
        assertNotNull( tps.getLandmarksJson(), "TPS landmarks JSON should be parsed and attached" );
        assertTrue( tps.getLandmarksJson().contains( "BigWarpLandmarks" ) );
    }

    @Test
    public void manyGroups()
    {
        // many-groups-collection.csv has 15 rows with distinct view names AND
        // distinct group values, producing 15 views each with its own group.
        Dataset dataset = buildDataset( "src/test/resources/collections/many-groups-collection.csv" );

        assertEquals( 15, dataset.views().size() );
        for ( int i = 1; i <= 15; i++ )
        {
            View view = dataset.views().get( "v" + i );
            assertNotNull( view, "expected view v" + i );
            assertTrue( view.getUiSelectionGroups().contains( "g" + i ),
                    "view v" + i + " should be assigned to group g" + i );
        }
    }

    @Test
    public void twoSameBlobs()
    {
        // two-same-blobs-collection.txt: same URI, two views ("a" and "b"),
        // different colors. Source-name disambiguation kicks in here too.
        Dataset dataset = buildDataset( "src/test/resources/collections/two-same-blobs-collection.txt" );

        assertEquals( 2, dataset.sources().size() );
        assertNotNull( dataset.sources().get( "blobs" ) );
        assertNotNull( dataset.sources().get( "blobs (1)" ) );

        assertNotNull( dataset.views().get( "a" ) );
        assertNotNull( dataset.views().get( "b" ) );
    }

    @Test
    public void boatsPng()
    {
        // boats-png-collection.txt has a single PNG URI — verifies PNG goes
        // through the same intensities path as TIFF (ImageDataFormat.BioFormats).
        Dataset dataset = buildDataset( "src/test/resources/collections/boats-png-collection.txt" );

        assertEquals( 1, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "boats" ) );
    }

    @Test
    public void segmentedBlobsGridTable()
    {
        // segmented-blobs-grid-table.txt has 4 rows: two intensities sources
        // and two labels sources, organised into two separate named grids
        // ("blob images" and "blob segmentations") in one view.
        Dataset dataset = buildDataset( "src/test/resources/collections/segmented-blobs-grid-table.txt" );

        assertEquals( 4, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "blobs1" ) );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "blobs2" ) );
        assertInstanceOf( SegmentationDataSource.class, dataset.sources().get( "labels1" ) );
        assertInstanceOf( SegmentationDataSource.class, dataset.sources().get( "labels2" ) );

        View view = dataset.views().get( "segmented blobs" );
        assertNotNull( view );

        // Two separate grids → two GridTransformations and two RegionDisplays.
        long grids = view.transformations().stream()
                .filter( t -> t instanceof GridTransformation ).count();
        assertEquals( 2, grids );

        long regionDisplays = view.displays().stream()
                .filter( d -> d instanceof RegionDisplay ).count();
        assertEquals( 2, regionDisplays );
    }

    @Test
    public void timelapseChannels()
    {
        // timelapse-collection.csv references xyzct-mitosis.tif twice with
        // different channel indices. Channel disambiguation appends a
        // "_ch<index>" suffix to the source name (see
        // CollectionDataSetter#getDataName and Constants.CHANNEL_POSTFIX).
        Dataset dataset = buildDataset( "src/test/resources/collections/timelapse-collection.csv" );

        assertEquals( 2, dataset.sources().size() );

        // The source name from the table is "mitosis_nuc" / "mitosis_mt",
        // each gets the channel postfix appended.
        boolean hasChannelSuffixedSources = dataset.sources().keySet().stream()
                .allMatch( n -> n.contains( "_ch" ) || n.matches( ".*\\d$" ) );
        assertTrue( hasChannelSuffixedSources,
                "each timelapse source should carry a channel postfix; got: " + dataset.sources().keySet() );

        // Both sources land in the same view "all" and same grid.
        View view = dataset.views().get( "all" );
        assertNotNull( view );
        assertEquals( 1, view.transformations().stream()
                .filter( t -> t instanceof GridTransformation ).count() );
    }

    @Test
    public void segmentedImageWithFloatLabels()
    {
        // segmented-image-collection.csv carries a UTF-8 BOM in the header,
        // a "labels" row with a labels_table, and a "view " header containing
        // a trailing space — exercises the column-name normalisation path.
        Dataset dataset = buildDataset( "src/test/resources/collections/segmented-image-collection.csv" );

        assertEquals( 2, dataset.sources().size() );
        assertInstanceOf( ImageDataSource.class, dataset.sources().get( "image_with_two_objects" ) );
        SegmentationDataSource labels = ( SegmentationDataSource )
                dataset.sources().get( "image_with_two_objects_float_labels" );
        assertNotNull( labels );
        assertNotNull( labels.getTableData() );
        assertTrue( ! labels.getTableData().isEmpty() );

        // Both rows specify view="segmented image" (trailing-space header notwithstanding).
        View view = dataset.views().get( "segmented image" );
        assertNotNull( view, "view name should be matched despite header whitespace/BOM" );
    }
}
