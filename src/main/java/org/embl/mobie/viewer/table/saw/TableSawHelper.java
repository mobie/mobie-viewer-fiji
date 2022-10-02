package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;

public class TableSawHelper
{
	public static Table readTable( String columnPath )
	{
		try
		{
			final String tableContent = IOHelper.read( columnPath );
			// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
			CsvReadOptions.Builder builder = CsvReadOptions.builderFromString( tableContent ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
			final Table rows = Table.read().usingOptions( builder );
			return rows;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
