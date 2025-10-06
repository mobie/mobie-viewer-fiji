package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.junit.Test;
import tech.tablesaw.api.Table;

import static org.junit.Assert.*;

public class TableOpenerTest {

    @Test
    public void testOpenExcelFile() throws Exception {

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
}
