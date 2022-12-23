package org.embl.mobie.viewer.table.saw;

import ij.measure.ResultsTable;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.columns.MorpholibJSegmentColumnNames;
import org.embl.mobie.viewer.table.columns.SegmentColumnNames;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TableOpener
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

	public static Table open( StorageLocation storageLocation, TableDataFormat tableDataFormat )
	{
		return open( storageLocation, storageLocation.defaultChunk, tableDataFormat, -1 );
	}

	public static Table open( StorageLocation storageLocation, String relativeChunkLocation, TableDataFormat tableDataFormat )
	{
		return open( storageLocation, relativeChunkLocation, tableDataFormat, -1 );
	}

	public static Table open( StorageLocation storageLocation, String relativeChunkLocation, TableDataFormat tableDataFormat, int numSamples )
	{
		switch ( tableDataFormat )
		{
			case MorpholibJCSV:
				// FIXME
				return null;
			case MorpholibJ:
				// FIXME
				return (Table) storageLocation.data;
			case MoBIETSV:
			default:
				return openMoBIETSV( storageLocation, relativeChunkLocation, tableDataFormat, numSamples );

		}

	}

	private static Table openMoBIETSV( StorageLocation storageLocation, String relativeChunkLocation, TableDataFormat tableDataFormat, int numSamples )
	{
		String path = IOHelper.combinePath( storageLocation.absolutePath, relativeChunkLocation );
		path = resolveTablePath( path );

		final Character separator = tableDataFormat.getSeparator();

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
			// FIXME: separator
			CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream ).separator( separator ).missingValueIndicator( "na", "none", "nan" ).sample( numSamples > 0 ).sampleSize( numSamples ).columnTypesPartial( nameToType );
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

	// Sometimes the path does not actually contain a table
	// but another link to a table
	// Example: https://raw.githubusercontent.com/mobie/platybrowser-datasets/mobie3/data/1.0.1/tables/sbem-6dpf-1-whole-segmented-ganglia/default.tsv
	public static String resolveTablePath( String tablePath )
	{
		if ( tablePath.startsWith( "http" ) ) {
			tablePath = IOHelper.resolveURL( URI.create( tablePath ) );
		} else {
			tablePath = IOHelper.resolvePath( tablePath );
		}
		return tablePath;
	}

	public static Table open( ResultsTable resultsTable, TableDataFormat tableDataFormat )
	{
		final SegmentColumnNames segmentColumnNames = tableDataFormat.getSegmentColumnNames();
		final String[] columnNames = resultsTable.getHeadings();

		final Table table = Table.create( resultsTable.getTitle() );
		for ( String columnName : columnNames )
		{
			if ( columnName.equals( segmentColumnNames.labelIdColumn() ) )
			{
				final int[] ints = Arrays.stream( resultsTable.getColumn( columnName ) ).mapToInt( x -> ( int ) x ).toArray();
				final IntColumn intColumn = IntColumn.create( columnName, ints );
				table.addColumns( intColumn );
			}
			else
			{
				final DoubleColumn doubleColumn = DoubleColumn.create( columnName, resultsTable.getColumn( columnName ) );
				table.addColumns( doubleColumn );
			}
		}

		return table;
	}
}
