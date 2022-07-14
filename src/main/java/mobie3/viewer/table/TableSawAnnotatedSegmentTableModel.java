package mobie3.viewer.table;

import net.imglib2.util.Pair;
import tech.tablesaw.api.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TableSawAnnotatedSegmentTableModel implements AnnotatedSegmentTableModel< TableSawAnnotatedSegment >
{
	protected Collection< String > columnPaths;
	protected LinkedHashSet< String > loadedColumnPaths;

	private HashMap< TableSawAnnotatedSegment, Integer > annotationToRowIndex;
	private HashMap< Integer, TableSawAnnotatedSegment > rowIndexToAnnotation;
	private Table table;

	public TableSawAnnotatedSegmentTableModel( String defaultColumnsPath )
	{
		loadedColumnPaths = new LinkedHashSet<>();
		annotationToRowIndex = new HashMap<>();
		rowIndexToAnnotation = new HashMap<>();

		loadedColumnPaths.add( defaultColumnsPath );
	}

	@Override
	public List< String > columnNames()
	{
		return table.columnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return TableSawColumnTypes.typeToClass.get( table.column( columnName ).type() );
	}

	@Override
	public int numRows()
	{
		return table.rowCount();
	}

	@Override
	public int indexOf( TableSawAnnotatedSegment annotation )
	{
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public TableSawAnnotatedSegment row( int rowIndex )
	{
		open();

		if ( ! rowIndexToAnnotation.containsKey( rowIndex ) )
		{
			final TableSawAnnotatedSegment annotation = new TableSawAnnotatedSegment( table, rowIndex );
			annotationToRowIndex.put( annotation, rowIndex );
			rowIndexToAnnotation.put( rowIndex, annotation );
		}

		return rowIndexToAnnotation.get( rowIndex );
	}

	private void open()
	{
		if ( table != null ) return;

		throw new UnsupportedOperationException("Table loading is not yet implemented.");
		// load table
		// think about the representation of missing values
		// e.g. should we use None or "" for a missing String?
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
		return null;
	}

	@Override
	public Set< TableSawAnnotatedSegment > rows()
	{
		return annotationToRowIndex.keySet();
	}
}
