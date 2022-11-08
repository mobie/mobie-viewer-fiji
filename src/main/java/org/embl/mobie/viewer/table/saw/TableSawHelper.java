package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TableSawHelper
{
	public static Map< String, ColumnType > nameToType;
	static
	{
		// FIXME Add more
		nameToType = new HashMap<>();
		nameToType.put( ColumnNames.SPOT_X, ColumnType.FLOAT );
		nameToType.put( ColumnNames.SPOT_Y, ColumnType.FLOAT );
		nameToType.put( ColumnNames.SPOT_Z, ColumnType.FLOAT );
	}

	public static Table readTable( String path, int numSamples )
	{
		// Sometimes the path does not actually contain a table
		// but another link to a table
		// Example: https://raw.githubusercontent.com/mobie/platybrowser-datasets/mobie3/data/1.0.1/tables/sbem-6dpf-1-whole-segmented-ganglia/default.tsv
		path = resolveTablePath( path );

		try
		{
			// Note that while it appears to be faster to
			// first load the whole table into one big string,
			// it has the drawback that we temporarily need to
			// allocate twice the memory and the GC has some
			// work to do, which can become a bottleneck.
			//System.out.println("Reading table " + path + "...");
			final long start = System.currentTimeMillis();
			final InputStream inputStream = IOHelper.getInputStream( path );
			//final String string = IOHelper.read( path );
			// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
			CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).sample( numSamples > 0 ).sampleSize( numSamples ).columnTypesPartial( nameToType );
			final Table rows = Table.read().usingOptions( builder );
			//System.out.println("Read table " + path + " with " + rows.rowCount() + " rows in " + ( System.currentTimeMillis() - start ) + " ms." );
			return rows;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	public static String resolveTablePath( String tablePath )
	{
		if ( tablePath.startsWith( "http" ) ) {
			tablePath = IOHelper.resolveURL( URI.create( tablePath ) );
		} else {
			tablePath = IOHelper.resolvePath( tablePath );
		}
		return tablePath;
	}
}
