package develop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetReader;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

public class ExploreTableSawParquetReading
{
    public static void main( String[] args )
    {
        ch.qos.logback.classic.Logger logger = ( Logger ) LoggerFactory.getLogger("org.apache.parquet.hadoop.InternalParquetRecordReader");
        logger.setLevel( Level.OFF );

        String tableFilePath = "/Users/tischer/Desktop/iss-nf/qc_spatialdata_processed/points/transcripts/points.parquet";

        // This is very slow because it logs all the rows that it is reading
        // https://github.com/tlabs-data/tablesaw-parquet/issues/75
        Table table = new TablesawParquetReader()
                .read( TablesawParquetReadOptions
                        .builder(tableFilePath)
                        .build() );

        System.out.println( "Read columns: " + String.join( ", ", table.columnNames() ) );
    }
}
