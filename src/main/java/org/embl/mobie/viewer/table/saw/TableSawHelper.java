package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import java.io.IOException;
import java.io.InputStream;

public class TableSawHelper
{

	public static Table readTable( String columnPath, int numSamples )
	{

		try
		{
			final long start = System.currentTimeMillis();
			final InputStream inputStream = IOHelper.getInputStream( columnPath );
			// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
			CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).sample( numSamples > 0 ).sampleSize( numSamples );
			final Table rows = Table.read().usingOptions( builder );
			System.out.println("Read table " + columnPath + " with " + rows.rowCount() + " rows in " + ( System.currentTimeMillis() - start ) + " ms." );
			return rows;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
