package org.embl.mobie.viewer.table.saw;

import ij.IJ;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AnnotationTableModel;
import org.embl.mobie.viewer.table.DefaultValues;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSawAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	private final TableSawAnnotationCreator< A > annotationCreator;
	private final String dataStore;
	protected Set< String > availableColumnPaths;
	protected LinkedHashSet< String > requestedColumnPaths = new LinkedHashSet<>();
	protected LinkedHashSet< String > loadedColumnPaths = new LinkedHashSet<>();

	private HashMap< A, Integer > annotationToRowIndex = new HashMap<>();;
	private HashMap< Integer, A > rowIndexToAnnotation = new HashMap<>();;
	private Table table;

	public TableSawAnnotationTableModel(
			TableSawAnnotationCreator< A > annotationCreator,
			String dataStore,
			String defaultColumn
	)
	{
		this.annotationCreator = annotationCreator;
		this.dataStore = dataStore;
		requestedColumnPaths.add( IOHelper.combinePath( dataStore, defaultColumn ) );
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html
	private synchronized Table table()
	{
		for ( String columnPath : requestedColumnPaths )
		{
			if ( loadedColumnPaths.contains( columnPath ) )
				continue;

			loadedColumnPaths.add( columnPath );

			try
			{
				//IJ.log( "Open table: " + columnPath );
				System.out.println( columnPath );
				final String tableContent = IOHelper.read( columnPath );
				// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
				CsvReadOptions.Builder builder = CsvReadOptions.builderFromString( tableContent ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
				final Table rows = Table.read().usingOptions( builder );

				if ( table == null )
				{
					this.table = rows;
					final int rowCount = table.rowCount();
					final ArrayList< Integer > dropRows = new ArrayList<>();
					for ( int rowIndex = 0, consecutiveRowIndex = 0; rowIndex < rowCount; rowIndex++ )
					{
						final A annotation = annotationCreator.create( () -> table, rowIndex );
						if ( annotation == null )
						{
							// This can happen for a region table, where
							// only a subset of rows is used.
							dropRows.add( rowIndex );
							continue;
						}
						annotationToRowIndex.put( annotation, consecutiveRowIndex );
						rowIndexToAnnotation.put( consecutiveRowIndex, annotation );
						consecutiveRowIndex++;
					}
					if ( dropRows.size() > 0 )
						table = table.dropRows( dropRows.stream().mapToInt( i -> i ).toArray() );
				}
				else
				{
					System.out.println( rows.columnNames() );
					table = table.joinOn( annotation( 0 ).idColumns()  ).inner( rows );
				}
			}
			catch ( IOException e )
			{
				throw new RuntimeException( e );
			}
		}

		return table;
	}

	private HashMap< A, Integer > annotationToRowIndex()
	{
		table(); // ensure data is loaded
		return annotationToRowIndex;
	}

	private HashMap< Integer, A > rowIndexToAnnotation()
	{
		table();
		return rowIndexToAnnotation;
	}

	@Override
	public List< String > columnNames()
	{
		return table().columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return table().numericColumns().stream().map( c -> c.name() ).collect( Collectors.toList() );
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return TableSawColumnTypes.typeToClass.get( table().column( columnName ).type() );
	}

	@Override
	public int numAnnotations()
	{
		return table().rowCount();
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return annotationToRowIndex().get( annotation );
	}

	@Override
	public A annotation( int rowIndex )
	{
		return rowIndexToAnnotation().get( rowIndex );
	}

	@Override
	public void requestColumns( String columnsPath )
	{
		requestedColumnPaths.add( columnsPath );
	}

	@Override
	public void setAvailableColumnPaths( Set< String > columnPaths )
	{
		this.availableColumnPaths = columnPaths;
	}

	@Override
	public Collection< String > availableColumnPaths()
	{
		if ( availableColumnPaths == null )
		{
			final String parentLocation = IOHelper.getParentLocation( requestedColumnPaths.iterator().next() );
			availableColumnPaths = Arrays.stream( IOHelper.getFileNames( parentLocation ) ).collect( Collectors.toSet() );
		}
		return availableColumnPaths;
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return requestedColumnPaths;
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		// one could consider caching the results...
		final NumericColumn< ? > numericColumn = table.nCol( columnName );
		final Summarizer summarize = table.summarize( numericColumn, AggregateFunctions.min, AggregateFunctions.max );
		final Table summary = summarize.apply();
		final ValuePair< Double, Double > minMax = new ValuePair( summary.get( 0, 0 ), summary.get( 0, 1  ) );
		return minMax;
	}

	@Override
	public Set< A > annotations()
	{
		return annotationToRowIndex().keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{
		if ( ! table().containsColumn( columnName ) )
		{
			final String[] strings = new String[ table().rowCount() ];
			Arrays.fill( strings, DefaultValues.NONE );
			final StringColumn stringColumn = StringColumn.create( columnName, strings );
			table().addColumns( stringColumn );
		}
		else
		{
			throw new UnsupportedOperationException("Column " + columnName + " exists already.");
		}
	}

	@Override
	public boolean isDataLoaded()
	{
		return table != null;
	}

	@Override
	public String dataStore()
	{
		return dataStore;
	}
}
