package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import tech.tablesaw.api.Table;

public class TableOpenerEncodingProbe
{
	public static void main( String[] args )
	{
		if ( args.length != 1 )
		{
			System.err.println( "Expected one argument: path to a collection CSV" );
			System.exit( 2 );
		}

		final String path = args[ 0 ];
		final TableDataFormat tableDataFormat = TableDataFormat.fromPath( path );
		final StorageLocation location = new StorageLocation();
		location.absolutePath = path;

		final Table table = TableOpener.open( location, tableDataFormat );
		if ( table.rowCount() != 2 )
		{
			System.err.println( "Unexpected row count: " + table.rowCount() );
			System.exit( 3 );
		}

		final String firstColumn = table.columnNames().get( 0 );
		if ( ! "uri".equals( firstColumn ) )
		{
			System.err.println( "Expected first column to be 'uri' but was: " + firstColumn );
			System.exit( 4 );
		}
	}
}

