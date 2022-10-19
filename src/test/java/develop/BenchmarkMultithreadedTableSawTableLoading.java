package develop;

import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.ThreadHelper;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BenchmarkMultithreadedTableSawTableLoading
{
	public static void main( String[] args ) throws IOException
	{
		long start;

		Table.read().usingOptions( CsvReadOptions.builderFromString( "aaa\tbbb" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ) );

		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		final ExecutorService ioExecutorService = Executors.newFixedThreadPool( 2 );
		start = System.currentTimeMillis();
		int n = 2;
		for ( int i = 0; i < n; i++ )
		{
			futures.add(
				ioExecutorService.submit( () ->
					{
						CsvReadOptions.Builder builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default_regions.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
						Table.read().usingOptions( builder );
					}
			) );
		}
		ThreadHelper.waitUntilFinished( futures );
		System.out.println("Build " + n + " tables [ms]: " + ( System.currentTimeMillis() - start ) );
	}
}
