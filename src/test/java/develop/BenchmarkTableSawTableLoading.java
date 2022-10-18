package develop;

import org.apache.commons.io.IOUtils;
import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BenchmarkTableSawTableLoading
{
	public static void main( String[] args ) throws IOException
	{
		long start;
		InputStream inputStream;
		CsvReadOptions.Builder builder;
		String tableString;

		final String tableURL = "https://raw.githubusercontent.com/mobie/spatial-transcriptomics-example-project/main/data/pos42/tables/transcriptome/default.tsv";

		System.out.println("Table source: " + tableURL );

		start = System.currentTimeMillis();
		tableString = IOHelper.read( tableURL );
		System.out.println("Read Table to String, using IOHelper.read [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		inputStream = new URL( tableURL ).openStream();
		System.out.println("Open Table InputStream [ms]: " + ( System.currentTimeMillis() - start ));

		builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		Table.read().usingOptions( builder );
		System.out.println("Build Table from InputStream [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		inputStream = new URL( tableURL ).openStream();
		System.out.println("Open InputStream [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		final String s1 = IOUtils.toString( inputStream, StandardCharsets.UTF_8.name() );
		System.out.println("Read InputStream to String, using IOUtils.toString [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		tableString = IOHelper.read( tableURL );
		System.out.println("Read Table to String, using IOHelper.read [ms]: " + ( System.currentTimeMillis() - start ));

		// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
		builder = CsvReadOptions.builderFromString( tableString ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		Table.read().usingOptions( builder );
		System.out.println("Parse Table from String [ms]: " + ( System.currentTimeMillis() - start ));
	}
}
