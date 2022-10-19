package develop;

import net.tlabs.tablesaw.parquet.TablesawParquet;
import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkTableSawTableLoading
{
	public static void main( String[] args ) throws IOException
	{
		long start;
		InputStream inputStream;
		CsvReadOptions.Builder builder;
		String tableString;
		Table table;

		//final String tableURL = "https://raw.githubusercontent.com/mobie/spatial-transcriptomics-example-project/main/data/pos42/tables/transcriptome/default.tsv";
		final ArrayList< Long > github = new ArrayList<>();
		for ( int i = 0; i < 20; i++ )
		{
			start = System.currentTimeMillis();
			//IOHelper.read( "https://raw.githubusercontent.com/mobie/spatial-transcriptomics-example-project/main/data/pos42/tables/transcriptome/default.tsv" );
			//IOHelper.read( "https://github.com/mobie/spatial-transcriptomics-example-project/raw/parquet/data/pos42/tables/transcriptome/default.parquet" );
			IOHelper.read( "https://raw.githubusercontent.com/mobie/spatial-transcriptomics-example-project/main/data/pos42/tables/positions/default.tsv" );
			github.add( System.currentTimeMillis() - start );
			System.out.println( "" + i );
		}
		final double average = github.stream().mapToLong( x -> x ).summaryStatistics().getAverage();
		System.out.println("github [ms]: " + average );

		final String tableSource = "/Users/tischer/Desktop/default.tsv";

		System.out.println("Table source: " + "/Users/tischer/Desktop/default_regions.tsv" );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default_regions.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start ) + " numRows=" + table.rowCount() );

		System.out.println("Table source: " + "/Users/tischer/Desktop/default.tsv" );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start ) + " numRows=" + table.rowCount() );

		System.out.println("Table source: " + "/Users/tischer/Desktop/default_regions.tsv" );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default_regions.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start ) + " numRows=" + table.rowCount() );

		System.out.println("Table source: " + "/Users/tischer/Desktop/default_regions.tsv"  );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default_regions.tsv"  ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start )+ " numRows=" + table.rowCount() );

		System.out.println("Table source: " + "/Users/tischer/Desktop/default.tsv" );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start ) + " numRows=" + table.rowCount() );

		System.out.println("Table source: " + "/Users/tischer/Desktop/default.tsv" );
		builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
		start = System.currentTimeMillis();
		table = Table.read().usingOptions( builder );
		System.out.println("Build Table from File [ms]: " + ( System.currentTimeMillis() - start ) + " numRows=" + table.rowCount() );

		start = System.currentTimeMillis();
		tableString = IOHelper.read( tableSource );
		System.out.println("Read Table to String, using IOHelper.read [ms]: " + ( System.currentTimeMillis() - start ));

		// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
		final ColumnType[] types = new ColumnType[ 9 ];
		int i = 0;
		types[ i++ ] = ColumnType.INTEGER;
		types[ i++ ] = ColumnType.STRING;
		types[ i++ ] = ColumnType.INTEGER;
		types[ i++ ] = ColumnType.DOUBLE;
		types[ i++ ] = ColumnType.DOUBLE;
		types[ i++ ] = ColumnType.INTEGER;
		types[ i++ ] = ColumnType.INTEGER;
		types[ i++ ] = ColumnType.INTEGER;
		types[ i++ ] = ColumnType.INTEGER;

		tableString = IOHelper.read( tableSource );

		final ArrayList< Long > with = new ArrayList<>();
		final ArrayList< Long > without = new ArrayList<>();

		for ( int j = 0; j < 10; j++ )
		{
			start = System.currentTimeMillis();
			builder = CsvReadOptions.builderFromString( tableString ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).columnTypes( types );
			final Table rows = Table.read().usingOptions( builder );
			final long l = System.currentTimeMillis() - start;
			with.add( l );
			System.out.println( "Parse Table from String (with types) [ms]: " + l + " numRows=" + table.rowCount() );

			start = System.currentTimeMillis();
			builder = CsvReadOptions.builderFromString( tableString ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
			final Table rows2 = Table.read().usingOptions( builder );
			final long l1 = System.currentTimeMillis() - start;
			without.add( l1 );
			System.out.println( "Parse Table from String (no types) [ms]: " + l1 + " numRows=" + table.rowCount() );
		}

		System.out.println( "with: " + with.stream().mapToLong( x -> x ).summaryStatistics().getAverage() );
		System.out.println( "without: " + without.stream().mapToLong( x -> x ).summaryStatistics().getAverage() );

		// CSV
		//
		final ArrayList< Long > csv = new ArrayList<>();
		for ( int j = 0; j < 20; j++ )
		{
			start = System.currentTimeMillis();
			final CsvReadOptions readOptions = CsvReadOptions.builder( "/Users/tischer/Desktop/default.tsv" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).build();
			final Table rows = Table.read().usingOptions( readOptions );
			final long l = System.currentTimeMillis() - start;
			if ( j > 3 )
				csv.add( l );
			//System.out.println( "Parse Table from CSV File (with types) [ms]: " + l + " numRows=" + table.rowCount() );
		}

		System.out.println( "CSV from file: " + csv.stream().mapToLong( x -> x ).summaryStatistics().getAverage() );


		// CSV from github with stream
		//
		final ArrayList< Long > csvStreamGithub = new ArrayList<>();
		for ( int j = 0; j < 20; j++ )
		{
			start = System.currentTimeMillis();
			inputStream = IOHelper.getInputStream( "https://raw.githubusercontent.com/mobie/spatial-transcriptomics-example-project/main/data/pos42/tables/transcriptome/default.tsv" );
			final CsvReadOptions readOptions = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ).build();
			final Table rows = Table.read().usingOptions( readOptions );
			final long l = System.currentTimeMillis() - start;
			//if ( j > 3 )
			csvStreamGithub.add( l );
			//System.out.println( "Parse Table from CSV File (with types) [ms]: " + l + " numRows=" + table.rowCount() );
		}

		System.out.println( "CSV from Github Stream, Max: " + csvStreamGithub.stream().mapToLong( x -> x ).summaryStatistics().getMax() );
		System.out.println( "CSV from Github Stream, Mean: " + csvStreamGithub.stream().mapToLong( x -> x ).summaryStatistics().getAverage() );


		// PARQUE
		//
//		TablesawParquet.register();
//
//		final ArrayList< Long > parquet = new ArrayList<>();
//		for ( int j = 0; j < 20; j++ )
//		{
//			start = System.currentTimeMillis();
//			final TablesawParquetReadOptions readOptions = TablesawParquetReadOptions.builder( "/Users/tischer/Desktop/default.parquet" ).build();
//			final Table rows = Table.read().usingOptions( readOptions );
//			final long l = System.currentTimeMillis() - start;
//			if ( j > 3 )
//				parquet.add( l );
//			//System.out.println( "Parse Table from String (with types) [ms]: " + l + " numRows=" + table.rowCount() );
//		}
//
//		System.out.println( "Parque from file: " + parquet.stream().mapToLong( x -> x ).summaryStatistics().getAverage() );

//		start = System.currentTimeMillis();
//		inputStream = new URL( tableURL ).openStream();
//		System.out.println("Open Table InputStream [ms]: " + ( System.currentTimeMillis() - start ));


//		builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
//		start = System.currentTimeMillis();
//		Table.read().usingOptions( builder );
//		System.out.println("Build Table from InputStream [ms]: " + ( System.currentTimeMillis() - start ));

//		start = System.currentTimeMillis();
//		inputStream = new URL( tableURL ).openStream();
//		System.out.println("Open InputStream [ms]: " + ( System.currentTimeMillis() - start ));

//		start = System.currentTimeMillis();
//		final String s1 = IOUtils.toString( inputStream, StandardCharsets.UTF_8.name() );
//		System.out.println("Read InputStream to String, using IOUtils.toString [ms]: " + ( System.currentTimeMillis() - start ));

	}
}
