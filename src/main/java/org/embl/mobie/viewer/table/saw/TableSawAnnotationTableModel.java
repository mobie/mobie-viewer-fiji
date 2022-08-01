package org.embl.mobie.viewer.table.saw;

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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSawAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	private final TableSawAnnotationCreator< A > annotationCreator;
	protected Set< String > availableColumnPaths = new HashSet<>();
	protected LinkedHashSet< String > loadedColumnPaths = new LinkedHashSet<>();

	private HashMap< A, Integer > annotationToRowIndex = new HashMap<>();;
	private HashMap< Integer, A > rowIndexToAnnotation = new HashMap<>();;
	private Table table;

	public TableSawAnnotationTableModel(
			TableSawAnnotationCreator< A > annotationCreator,
			String defaultColumnsPath
	)
	{
		this.annotationCreator = annotationCreator;
		availableColumnPaths.add( defaultColumnsPath );
		loadedColumnPaths.add( defaultColumnsPath );
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html
	private Table getTable()
	{
		// TODO: MUST implement column merging
		if ( table == null )
		{
			for ( String columnPath : loadedColumnPaths() )
			{
				try
				{
					final String tableContent = IOHelper.read( columnPath );
					// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
					CsvReadOptions.Builder builder = CsvReadOptions.builderFromString( tableContent ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
					this.table = Table.read().usingOptions( builder );
					final int rowCount = table.rowCount();
					for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
					{
						final A annotation = annotationCreator.create( table.row( rowIndex ) );
						annotationToRowIndex.put( annotation, rowIndex );
						rowIndexToAnnotation.put( rowIndex, annotation );
					}
				}
				catch ( IOException e )
				{
					throw new RuntimeException( e );
				}
			}
		}

		return table;
	}

	@Override
	public List< String > columnNames()
	{
		return getTable().columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return getTable().numericColumns().stream().map( c -> c.name() ).collect( Collectors.toList() );
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return TableSawColumnTypes.typeToClass.get( getTable().column( columnName ).type() );
	}

	@Override
	public int numAnnotations()
	{
		return getTable().rowCount();
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public A annotation( int rowIndex )
	{
		getTable(); // ensures that the data is loaded
		return rowIndexToAnnotation.get( rowIndex );
	}

	@Override
	public void loadColumns( String columnsPath )
	{
		loadedColumnPaths.add( columnsPath );
	}

	@Override
	public void setColumnPaths( Set< String > columnPaths )
	{
		this.availableColumnPaths = columnPaths;
	}

	@Override
	public Collection< String > columnPaths()
	{
		return availableColumnPaths;
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return loadedColumnPaths;
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		// TODO: cache results?!
		final NumericColumn< ? > numericColumn = table.nCol( columnName );
		final Summarizer summarize = table.summarize( numericColumn, AggregateFunctions.min, AggregateFunctions.max );
		final Table summary = summarize.apply();
		final ValuePair< Double, Double > minMax = new ValuePair( summary.get( 0, 0 ), summary.get( 0, 1  ) );
		return minMax;
	}

	@Override
	public Set< A > annotations()
	{
		return annotationToRowIndex.keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{
		if ( ! getTable().containsColumn( columnName ) )
		{
			final String[] strings = new String[ getTable().rowCount() ];
			Arrays.fill( strings, DefaultValues.NONE );
			final StringColumn stringColumn = StringColumn.create( columnName, strings );
			getTable().addColumns( stringColumn );
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
}
