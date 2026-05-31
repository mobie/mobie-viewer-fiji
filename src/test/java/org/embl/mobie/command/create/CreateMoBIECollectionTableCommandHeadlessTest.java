package org.embl.mobie.command.create;

import org.embl.mobie.lib.create.CollectionTableCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.tablesaw.api.Table;

import java.io.File;
import java.nio.file.Path;

import static org.embl.mobie.command.create.CreateMoBIECollectionTableCommand.GRID;
import static org.embl.mobie.command.create.CreateMoBIECollectionTableCommand.INDIVIDUAL;
import static org.embl.mobie.command.create.CreateMoBIECollectionTableCommand.TOGETHER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the create-collection-table command.
 *
 * Bypasses the UI ({@code IJ.open(...)} and the optional follow-up MoBIE
 * viewer launch in {@link CreateMoBIECollectionTableCommand#run()}) by
 * driving {@link CollectionTableCreator} directly, which is the core
 * data-producing step of the command.
 *
 * The UI-driving variants of these tests are kept in
 * {@code CreateMoBIECollectionTableCommandTest} (its {@code main}) for manual
 * visual verification.
 */
public class CreateMoBIECollectionTableCommandHeadlessTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private static final File CONTROL = new File( "src/test/resources/collections/control--000.tif" );
    private static final File TREATED = new File( "src/test/resources/collections/treated--000.tif" );

    private static final String DEFAULT_REGEX = "(?<condition>.*)--(?<replicate>.*).tif";

    @Test
    public void gridLayoutProducesGridColumnsAndPositions( @TempDir Path tempDir )
    {
        Table table = new CollectionTableCreator(
                new File[]{ CONTROL, TREATED },
                tempDir.toFile(),
                GRID,
                DEFAULT_REGEX
        ).createTable();

        // Grid layout must add "grid" and "grid_position" columns.
        assertTrue( table.containsColumn( "grid" ) );
        assertTrue( table.containsColumn( "grid_position" ) );

        // Both test TIFFs have 2 datasets (channels) each, so 2 files * 2 datasets = 4 rows.
        assertEquals( 4, table.rowCount() );

        // All rows are in the same grid named "grid".
        for ( int i = 0; i < table.rowCount(); i++ )
            assertEquals( "grid", table.getString( i, "grid" ) );

        // Grid positions are per file (one position re-used for every dataset
        // inside that file). With 2 files, ceil(sqrt(2))=2, so file 0 -> (0,0),
        // file 1 -> (1,0); each repeated twice for the two datasets.
        assertEquals( "(0,0)", table.getString( 0, "grid_position" ) );
        assertEquals( "(0,0)", table.getString( 1, "grid_position" ) );
        assertEquals( "(1,0)", table.getString( 2, "grid_position" ) );
        assertEquals( "(1,0)", table.getString( 3, "grid_position" ) );

        // All rows share the same view name "all data" for GRID/TOGETHER layout.
        for ( int i = 0; i < table.rowCount(); i++ )
            assertEquals( "all data", table.getString( i, "view" ) );

        // The regex groups "condition" and "replicate" become extra columns.
        // Per-dataset rows inherit the per-file regex match.
        assertTrue( table.containsColumn( "condition" ) );
        assertTrue( table.containsColumn( "replicate" ) );
        assertEquals( "control", table.getString( 0, "condition" ) );
        assertEquals( "control", table.getString( 1, "condition" ) );
        assertEquals( "treated", table.getString( 2, "condition" ) );
        assertEquals( "treated", table.getString( 3, "condition" ) );

        // Standard columns must be present.
        for ( String col : new String[]{ "uri", "channel", "display", "color",
                "contrast_limits", "exclusive", "pixel_size_x", "num_pixels_x" } )
            assertTrue( table.containsColumn( col ), "missing column: " + col );
    }

    @Test
    public void togetherLayoutOmitsGridColumns( @TempDir Path tempDir )
    {
        Table table = new CollectionTableCreator(
                new File[]{ CONTROL, TREATED },
                tempDir.toFile(),
                TOGETHER,
                DEFAULT_REGEX
        ).createTable();

        // TOGETHER layout does NOT add grid columns.
        assertEquals( false, table.containsColumn( "grid" ) );
        assertEquals( false, table.containsColumn( "grid_position" ) );

        // All rows still share the "all data" view (same as GRID).
        for ( int i = 0; i < table.rowCount(); i++ )
            assertEquals( "all data", table.getString( i, "view" ) );
    }

    @Test
    public void individualLayoutAssignsPerFileViewNames( @TempDir Path tempDir )
    {
        Table table = new CollectionTableCreator(
                new File[]{ CONTROL, TREATED },
                tempDir.toFile(),
                INDIVIDUAL,
                DEFAULT_REGEX
        ).createTable();

        // INDIVIDUAL layout uses the file's basename (without extension) as the
        // view. Each file contributes 2 dataset rows, so rows 0–1 belong to
        // "control--000" and rows 2–3 to "treated--000".
        assertEquals( 4, table.rowCount() );
        assertEquals( "control--000", table.getString( 0, "view" ) );
        assertEquals( "control--000", table.getString( 1, "view" ) );
        assertEquals( "treated--000", table.getString( 2, "view" ) );
        assertEquals( "treated--000", table.getString( 3, "view" ) );

        // INDIVIDUAL layout also omits grid columns.
        assertEquals( false, table.containsColumn( "grid" ) );
        assertEquals( false, table.containsColumn( "grid_position" ) );
    }

    @Test
    public void roundTripThroughHeadlessOpener( @TempDir Path tempDir ) throws Exception
    {
        // End-to-end sanity check: write the generated table to disk and then
        // re-open it via the headless collection-table parsing path, ensuring
        // the produced CSV is consumable by MoBIE's own opener.
        Table created = new CollectionTableCreator(
                new File[]{ CONTROL, TREATED },
                tempDir.toFile(),
                GRID,
                DEFAULT_REGEX
        ).createTable();

        File csv = new File( tempDir.toFile(), "out.csv" );
        created.write().csv( csv );
        assertTrue( csv.exists() && csv.length() > 0 );

        // Reopen and confirm it parses with the same number of rows.
        Table reopened = org.embl.mobie.lib.table.saw.TableOpener.open( csv.getAbsolutePath() );
        assertNotNull( reopened );
        assertEquals( created.rowCount(), reopened.rowCount() );
        assertTrue( reopened.columnNames().contains( "uri" ) );
        assertTrue( reopened.columnNames().contains( "grid" ) );
    }
}
