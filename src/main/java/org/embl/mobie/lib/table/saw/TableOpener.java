/*
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.table.saw;

import ij.IJ;
import ij.measure.ResultsTable;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetReader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.BufferedReader;
import java.io.FileReader;
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

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
			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/935
			case ResultsTable:
				return openResultTable( (ResultsTable) storageLocation.data );
			case Table:
				return (Table) storageLocation.data;
			case TSV:
			case CSV:
			default:
				return openTableFile( storageLocation, chunk, tableDataFormat );
		}
	}

	private static Table openTableFile( StorageLocation storageLocation, String relativeChunkLocation, TableDataFormat tableDataFormat )
	{
		return openTableFile( storageLocation, relativeChunkLocation, tableDataFormat, -1 );
	}

	private static Table openTableFile( StorageLocation storageLocation, String chunk, TableDataFormat tableDataFormat, int numSamples )
	{
		if ( tableDataFormat.equals( TableDataFormat.PARQUET ) )
		{
			ch.qos.logback.classic.Logger logger = ( Logger ) LoggerFactory.getLogger("org.apache.parquet.hadoop.InternalParquetRecordReader");
			logger.setLevel( Level.OFF );

			Table table = new TablesawParquetReader()
					.read( TablesawParquetReadOptions
							.builder( storageLocation.absolutePath )
							.build() );

			System.out.println( "Read parquet table with columns:\n" + String.join( ", ", table.columnNames() ) );

			return table;
		}
		else
		{
			final String path = resolveTablePath( IOHelper.combinePath( storageLocation.absolutePath, chunk ) );
			final Character separator = tableDataFormat.getSeparator();
			return openDelimitedTextFile( numSamples, path, separator );
		}
	}

	private static Table openDelimitedTextFile( int numSamples, String path, Character separator )
	{
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
			Table table = Table.read().usingOptions( builder );
			//System.out.println("Read table " + path + " with " + rows.rowCount() + " rows in " + ( System.currentTimeMillis() - start ) + " ms." );
			return table;
		}
		catch ( Exception e )
		{
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
				int[] labels;
				try
				{
					labels = Arrays.stream( resultsTable.getColumn( columnName ) ).mapToInt( x -> ( int ) x ).toArray();
				}
				catch ( Exception e )
				{
					// "Labels" can be a special column that cannot be accessed
					// as a normal column
					final int size = resultsTable.size();
					labels = new int[ size ];
					for ( int i = 0; i < size; i++ )
						labels[ i ] = Integer.parseInt( resultsTable.getLabel( i ) );

				}
				final IntColumn intColumn = IntColumn.create( columnName, labels );
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

	public static Character determineDelimiter( String path )
	{
		if ( path.endsWith( ".txt" ) )
			return '\t';

		if ( path.endsWith( ".tsv" ) )
			return '\t';

		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String firstLine = reader.readLine();
			if (firstLine != null) {
				if (firstLine.contains("\t")) {
					return '\t';
				} else if (firstLine.contains(",")) {
					return ',';
				} else {
					return ','; // We could throw an error, but maybe there is only one column ?
				}
			}
		} catch (IOException e) {
			throw new RuntimeException( e );
		}

		throw new RuntimeException( "Could not determine table delimiter" );
	}

	public static Table openDelimitedTextFile( String path, char separator )
	{
		String content = readContent( path );

		// FIXME: This is brittle; don't always do this, this is only for CellProfiler!
		// content = dealWithTwoHeaderRowsIfNeeded( separator, content );

		CsvReadOptions.Builder builder =
				CsvReadOptions.builderFromString( content )
						.separator( separator )
						.missingValueIndicator( "na", "none", "nan" );

		return Table.read().usingOptions( builder );
	}

	private static String readContent( String path )
	{
		String content = null;
		try
		{
			content = IOHelper.read( path );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
		return content;
	}

	private static String dealWithTwoHeaderRowsIfNeeded( char separator, String content )
	{
		String[] lines = content.split( "\\r?\\n" );

		final String[] columns = lines[ 0 ].split( "" + separator );
		final Map< String, Long > collect = Arrays.stream( columns )
				.collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
		boolean containsDuplicateColumnNames = false;

		for ( String column : collect.keySet() )
		{
			if ( collect.get( column ) > 1 )
			{
				IJ.log("[WARNING] Found duplicate column names, e.g.: " + column );
				containsDuplicateColumnNames = true;
				break;
			}
		}

		// FIXME: this may not always be desired, maybe we just want to throw an error here...
		//        maybe better to explicitly figure out whether this is a CellProfiler table?
		if ( containsDuplicateColumnNames )
		{
			IJ.log("[WARNING] Trying now to open the table assuming that there are two header rows...");

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
			for ( int i = 2; i < lines.length; i++ )
			{
				lineList.add( lines[ i ] );
			}

			content = Strings.join( System.lineSeparator(), lineList );
		}
		return content;
	}

	public static Table openDelimitedTextFile( String path )
	{
		return openDelimitedTextFile( path, determineDelimiter( path ) );
	}
}
