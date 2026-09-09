package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableOpenerTest {

    @Test
    public void testOpenExcelFile() {

         // Call the method under test
        TableDataFormat tableDataFormat = TableDataFormat.fromPath( "src/test/resources/collections/clem-collection.xlsx" );
        StorageLocation location = new StorageLocation();
        location.absolutePath = "src/test/resources/collections/clem-collection.xlsx";
        Table table = TableOpener.open( location, tableDataFormat );

        //Table table = TableOpener.openExcelFile("src/test/resources/test.xlsx");

        // Verify the table structure
        assertNotNull(table);
        int rowCount = table.rowCount();
        assertEquals(2, rowCount);
        assertEquals("uri", table.columnNames().get(0));
        assertEquals("affine", table.columnNames().get(1));
    }

    @Test
    public void testIsMoBIECollectionTableFromUri() {
        assertTrue( TableOpener.isMoBIECollectionTable( "src/test/resources/collections/blobs-collection.txt" ) );
        assertFalse( TableOpener.isMoBIECollectionTable( "src/test/resources/collections/blobs.tif" ) );
    }

    @Test
    public void testIsMoBIECollectionTableFromTable() {

        //boolean moBIECollectionTable = TableOpener.isMoBIECollectionTable( "https://docs.google.com/spreadsheets/d/1hj_JKnBLp1nJzeSG6mcL6INIsFH2meKzNx59vmYL53Y/edit?gid=0#gid=0" );

        Table withUri = Table.create( "with-uri" )
                .addColumns( StringColumn.create( "uri", "x.tif" ) );
        assertTrue( TableOpener.isMoBIECollectionTable( withUri ) );

        Table withAlias = Table.create( "with-alias" )
                .addColumns( StringColumn.create( "File Path", "x.tif" ) );
        assertTrue( TableOpener.isMoBIECollectionTable( withAlias ) );

        Table withoutRequiredColumn = Table.create( "without-uri" )
                .addColumns( StringColumn.create( CollectionTableConstants.NAME, "x" ) );
        assertFalse( TableOpener.isMoBIECollectionTable( withoutRequiredColumn ) );
    }
}
