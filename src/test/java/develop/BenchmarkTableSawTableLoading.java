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

		final String tableURL = "https://raw.githubusercontent.com/mobie/platybrowser-project/main/data/1.0.1/tables/sbem-6dpf-1-whole-segmented-cells/default.tsv";

		start = System.currentTimeMillis();
		URL url = new URL(tableURL);
		final InputStream inputStream = url.openStream();
		System.out.println("Open Table InputStream [ms]: " + ( System.currentTimeMillis() - start ));

//		ByteArrayOutputStream result = new ByteArrayOutputStream();
//		byte[] buffer = new byte[1024];
//		for (int length; (length = inputStream.read(buffer)) != -1; ) {
//			result.write(buffer, 0, length);
//		}
//		final String s0 = result.toString("UTF-8");
//		System.out.println("Read InputStream in String, using ByteArrayOutputStream [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		final String s1 = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		System.out.println("Read InputStream in String, using IOUtils.toString [ms]: " + ( System.currentTimeMillis() - start ));

		start = System.currentTimeMillis();
		final String tableString = IOHelper.read( tableURL );
		System.out.println("Read Table [ms]: " + ( System.currentTimeMillis() - start ));


		// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
		CsvReadOptions.Builder builder = CsvReadOptions.builderFromString( tableString ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		final Table table = Table.read().usingOptions( builder );
		System.out.println("Build Table from String [ms]: " + ( System.currentTimeMillis() - start ));
	}
}
