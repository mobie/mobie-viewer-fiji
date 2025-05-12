package org.embl.mobie.lib.table.saw;

import develop.ExploreJsonBookmarksParsing;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.junit.Test;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class TableOpenerTest {

    @Test
    public void testOpenExcelFile() throws Exception {

         // Call the method under test
        TableDataFormat tableDataFormat = TableDataFormat.fromPath( "src/test/resources/test.xlsx" );
        StorageLocation location = new StorageLocation();
        location.absolutePath = "src/test/resources/test.xlsx";
        Table table = TableOpener.open( location, tableDataFormat );

        //Table table = TableOpener.openExcelFile("src/test/resources/test.xlsx");

        // Verify the table structure
        assertNotNull(table);
        assertEquals("test", table.columnNames().get(0));
    }
}
