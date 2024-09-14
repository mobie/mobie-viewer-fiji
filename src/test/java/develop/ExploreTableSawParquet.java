package develop;

import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetReader;
import net.tlabs.tablesaw.parquet.TablesawParquetWriteOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetWriter;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class ExploreTableSawParquet
{
    public static void main( String[] args )
    {
        String tableFilePath = "/Users/tischer/Desktop/table.parquet";

        // Create a Tablesaw table
        StringColumn name = StringColumn.create("Name", new String[] {"Alice", "Bob", "Charlie"});
        IntColumn age = IntColumn.create("Age", new int[] {30, 25, 35});
        Table table = Table.create("People", name, age);

        // Write as Parquet
        new TablesawParquetWriter().
                write(table,
                TablesawParquetWriteOptions
                        .builder( tableFilePath )
                        .withOverwrite(true).build() );

        // Read only a subset of the columns,
        // which is possible to due the parquet format
        table = new TablesawParquetReader()
                .read( TablesawParquetReadOptions
                        .builder( tableFilePath )
                        .withOnlyTheseColumns( "Name" ).build() );

        System.out.println( "Read columns: " + String.join( ",", table.columnNames() ) );
    }
}
