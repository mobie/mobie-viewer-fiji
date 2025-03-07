package develop;

import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetReader;
import net.tlabs.tablesaw.parquet.TablesawParquetWriteOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetWriter;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

public class ExploreTableSawParquet
{
    public static void main( String[] args )
    {
        String tableFilePath = "/Users/tischer/Desktop/table.parquet";

        // Create a Tablesaw table
        IntColumn labelId = IntColumn.create( "label_id", 1, 2, 3 );
        DoubleColumn area = DoubleColumn.create("area_um2", 100.0, 123.5, 115.3 );
        DoubleColumn circularity = DoubleColumn.create("circularity", 0.95, 0.43, 0.77 );

        Table table = Table.create("cells", labelId, area, circularity);

        // Write as Parquet
        new TablesawParquetWriter().
                write(table,
                TablesawParquetWriteOptions
                        .builder(tableFilePath)
                        .withOverwrite(true).build() );

        // Read only a subset of the columns,
        // which is possible to due the parquet format
        table = new TablesawParquetReader()
                .read( TablesawParquetReadOptions
                        .builder(tableFilePath)
                        .withOnlyTheseColumns("label_id", "area_um2").build() );

        System.out.println( "Read columns: " + String.join( ", ", table.columnNames() ) );
    }
}
