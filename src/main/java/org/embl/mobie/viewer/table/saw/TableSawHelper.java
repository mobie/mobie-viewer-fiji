package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import java.io.IOException;
import java.io.InputStream;

public class TableSawHelper
{

	public static Table readTable( String path, int numSamples )
	{
		try
		{
			// Note that while it appears to be faster to
			// first load the whole table into one big string,
			// it has the drawback that we temporarily need to
			// allocate twice the memory and the GC has some
			// work to do, which can become a bottleneck.
			System.out.println("Reading table " + path + "...");
			final long start = System.currentTimeMillis();
			final InputStream inputStream = IOHelper.getInputStream( path );
			//final String string = IOHelper.read( path );
			// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
			CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).sample( numSamples > 0 ).sampleSize( numSamples );
			final Table rows = Table.read().usingOptions( builder );
			System.out.println("Read table " + path + " with " + rows.rowCount() + " rows in " + ( System.currentTimeMillis() - start ) + " ms." );
			return rows;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
