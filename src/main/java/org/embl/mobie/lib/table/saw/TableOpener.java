package org.embl.mobie.lib.table.saw;

import IceInternal.Ex;
import ij.IJ;
import ij.measure.ResultsTable;
import net.thisptr.jackson.jq.internal.misc.Strings;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TableOpener
{
	public static Map< String, ColumnType > nameToType;
	static
	{
		// TODO Add more
		// TODO Get this from the ColumnNames instead?
		nameToType = new HashMap<>();
		nameToType.put( ColumnNames.SPOT_X, ColumnType.FLOAT );
		nameToType.put( ColumnNames.SPOT_Y, ColumnType.FLOAT );
		nameToType.put( ColumnNames.SPOT_Z, ColumnType.FLOAT );
	}

	public static Table open( StorageLocation storageLocation, TableDataFormat tableDataFormat )
	{
		return open( storageLocation, storageLocation.defaultChunk, tableDataFormat );
	}

	public static Table open( StorageLocation storageLocation, String chunk, TableDataFormat tableDataFormat )
	{
		switch ( tableDataFormat )
		{
			// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/935
			case ResultsTable:
				return openResultTable( (ResultsTable) storageLocation.data );
			case Table:
				return (Table) storageLocation.data;
			case TSV:
			case CSV:
			default:
				return openFile( storageLocation, chunk, tableDataFormat );
		}
	}

	private static Table openFile( StorageLocation storageLocation, String relativeChunkLocation, TableDataFormat tableDataFormat )
	{
		return openFile( storageLocation, relativeChunkLocation, tableDataFormat, -1 );
	}

	private static Table openFile( StorageLocation storageLocation, String chunk, TableDataFormat tableDataFormat, int numSamples )
	{
		final String path = resolveTablePath( IOHelper.combinePath( storageLocation.absolutePath, chunk ) );
		final Character separator = tableDataFormat.getSeparator();

		try
		{
			// while it appears to be faster to
			// first load the whole table into one big string,
			// it has the drawback that we temporarily need to
			// allocate twice the memory and the GC has some
			// work to do, which can become a bottleneck.
			final long start = System.currentTimeMillis();
			final InputStream inputStream = IOHelper.getInputStream( path );
			// final String string = IOHelper.read( path );
			// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
			CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream )
					.separator( separator )
					.missingValueIndicator( "na", "none", "nan" )
					.sample( numSamples > 0 )
					.sampleSize( numSamples )
					.columnTypesPartial( nameToType );
			final Table rows = Table.read().usingOptions( builder );
			//System.out.println("Read table " + path + " with " + rows.rowCount() + " rows in " + ( System.currentTimeMillis() - start ) + " ms." );
			return rows;
		}
		catch ( Exception e )
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

	// convert ImageJ ResultsTable to tableSaw Table
	public static Table openResultTable( ResultsTable resultsTable )
	{
		final String[] columnNames = resultsTable.getHeadings();
		final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( Arrays.asList( columnNames ) );

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

	public static Character guessDelimiter( String path )
	{
		if ( path.endsWith( ".txt" ) )
			return '\t';

		if ( path.endsWith( ".tsv" ) )
			return '\t';

		return ',';
	}

	public static Table openDelimitedTextFile( String path, char separator )
	{
		String content = null;
		try
		{
			content = IOHelper.read( path );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}

		content = fixMultipleHeaderRows( separator, content );

		CsvReadOptions.Builder builder =
				CsvReadOptions.builderFromString( content )
						.separator( separator )
						.missingValueIndicator( "na", "none", "nan" );

		return Table.read().usingOptions( builder );
	}

	private static String fixMultipleHeaderRows( char separator, String content )
	{
		String[] lines = content.split( System.lineSeparator() );
		final String[] columns = lines[ 0 ].split( "" + separator );
		final Map< String, Long > collect = Arrays.stream( columns ).collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
		boolean containsDuplicateColumnNames = false;
		for ( String column : collect.keySet() )
		{
			if ( collect.get( column ) > 1 )
			{
				IJ.log("[WARNING] found duplicate column names: " + column );
				containsDuplicateColumnNames = true;
				break;
			}
		}

		if ( containsDuplicateColumnNames )
		{
			IJ.log("[WARNING] assuming that this to be a table with two header rows.");

			final String[] columns2 = lines[ 1 ].split( "" + separator );
			final String[] combinedColumns = new String[ columns.length ];
			for ( int i = 0; i < columns.length; i++ )
			{
				// for some reason the \r are not removed from the last column names
				// when splitting the lines, thus we need to replace them here
				combinedColumns[ i ] = columns[ i ].replace( "\r", "" ) + "_" + columns2[ i ].replace( "\r", "" );
			}

			final String header = Strings.join( "" + separator, Arrays.asList( combinedColumns ) );
			final List< String > lineList = new ArrayList<>();
			lineList.add( header );
			for ( int i = 2; i < 3; i++ )
			{
				lineList.add( lines[ i ] );
			}

			content = Strings.join( System.lineSeparator(), lineList );
		}
		return content;
	}

	public static Table openDelimitedTextFile( String path )
	{
		return openDelimitedTextFile( path, guessDelimiter( path ) );
	}
}
