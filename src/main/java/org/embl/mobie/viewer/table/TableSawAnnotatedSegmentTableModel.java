package org.embl.mobie.viewer.table;

import lombok.SneakyThrows;
import net.imglib2.util.Pair;
import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSawAnnotatedSegmentTableModel implements AnnotationTableModel< TableSawAnnotatedSegment >
{
	protected Collection< String > availableColumnPaths;
	protected LinkedHashSet< String > loadedColumnPaths;

	private HashMap< TableSawAnnotatedSegment, Integer > annotationToRowIndex;
	private HashMap< Integer, TableSawAnnotatedSegment > rowIndexToAnnotation;
	private Table table;
	private boolean isDataLoaded = false;

	public TableSawAnnotatedSegmentTableModel( String defaultColumnsPath )
	{
		loadedColumnPaths = new LinkedHashSet<>();
		annotationToRowIndex = new HashMap<>();
		rowIndexToAnnotation = new HashMap<>();

		loadedColumnPaths.add( defaultColumnsPath );
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html
	private Table getTable()
	{
		if ( table == null )
		{
			for ( String columnPath : loadedColumnPaths() )
			{
				try
				{
					final InputStream inputStream = IOHelper.getInputStream( columnPath );
					// https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
					CsvReadOptions.Builder builder = CsvReadOptions.builder( inputStream ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
					final Table table = Table.read().usingOptions( builder );
					if ( this.table == null )
					{
						this.table = table;
						final int rowCount = table.rowCount();
						for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
						{
							final TableSawAnnotatedSegment annotation = new TableSawAnnotatedSegment( table, rowIndex );
							annotationToRowIndex.put( annotation, rowIndex );
							rowIndexToAnnotation.put( rowIndex, annotation );
						}
					}
					else
					{
						throw new UnsupportedOperationException("Merging additional columns is not yet supported.");
						// TODO: merging of columns
						// https://www.javadoc.io/doc/tech.tablesaw/tablesaw-core/0.34.1/tech/tablesaw/joining/DataFrameJoiner.html
					}
				} catch ( IOException e )
				{
					throw new RuntimeException( e );
				}
			}
		}

		isDataLoaded = true;
		return table;

		// load table
//		annotationToRowIndex.put( annotation, rowIndex );
//		rowIndexToAnnotation.put( rowIndex, annotation );
//
		// think about the representation of missing values
		// e.g. should we use None or "" for a missing String?
		// return table;

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
	public int numRows()
	{
		return getTable().rowCount();
	}

	@Override
	public int indexOf( TableSawAnnotatedSegment annotation )
	{
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public TableSawAnnotatedSegment row( int rowIndex )
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
	public void setColumnPaths( Collection< String > columnPaths )
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
		// TODO: cache results (unless TableSaw caches it?)
		return null;
	}

	@Override
	public Set< TableSawAnnotatedSegment > rows()
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
		return isDataLoaded;
	}
}
