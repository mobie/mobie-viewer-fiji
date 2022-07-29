package mobie3.viewer.table;

import net.imglib2.util.Pair;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSawAnnotatedSegmentTableModel implements AnnotationTableModel< TableSawAnnotatedSegment >
{
	protected Collection< String > columnPaths;
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

	private Table getTable()
	{
		if ( table != null ) return table;

		// load table
		isDataLoaded = true;

//		annotationToRowIndex.put( annotation, rowIndex );
//		rowIndexToAnnotation.put( rowIndex, annotation );
//
		// think about the representation of missing values
		// e.g. should we use None or "" for a missing String?
		// return table;

		throw new UnsupportedOperationException("Table loading is not yet implemented.");


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

		if ( ! rowIndexToAnnotation.containsKey( rowIndex ) )
		{
			final TableSawAnnotatedSegment annotation = new TableSawAnnotatedSegment( getTable(), rowIndex );
			annotationToRowIndex.put( annotation, rowIndex );
			rowIndexToAnnotation.put( rowIndex, annotation );
		}

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
		this.columnPaths = columnPaths;
	}

	@Override
	public Collection< String > columnPaths()
	{
		return columnPaths;
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
