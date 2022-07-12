package mobie3.viewer.table;

import net.imglib2.util.Pair;
import tech.tablesaw.api.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TableSawSegmentsTableModel implements SegmentsTableModel< TableSawAnnotatedSegment >
{
	protected final String columnsPath;
	protected Collection< String > columnPaths;
	protected List< String > loadedColumnPaths;

	private HashMap< TableSawAnnotatedSegment, Integer > annotationToRowIndex;
	private HashMap< Integer, TableSawAnnotatedSegment > rowIndexToAnnotation;
	private Table table;

	public TableSawSegmentsTableModel( String columnsPath )
	{
		this.columnsPath = columnsPath;
		annotationToRowIndex = new HashMap<>();
		rowIndexToAnnotation = new HashMap<>();
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
	public int getRowIndex( TableSawAnnotatedSegment annotation )
	{
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public TableSawAnnotatedSegment getRow( int rowIndex )
	{
		if ( ! rowIndexToAnnotation.containsKey( rowIndex ) )
		{
			final TableSawAnnotatedSegment annotation = new TableSawAnnotatedSegment( table.row( rowIndex ) );
			annotationToRowIndex.put( annotation, rowIndex );
			rowIndexToAnnotation.put( rowIndex, annotation );
		}

		return rowIndexToAnnotation.get( rowIndex );
	}

	@Override
	public void loadColumns( String columnsPath )
	{

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
	public List< String > loadedColumnPaths()
	{
		return loadedColumnPaths;
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		return null;
	}
}
