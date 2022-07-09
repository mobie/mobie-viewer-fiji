package mobie3.viewer.table;

import net.imglib2.util.Pair;
import tech.tablesaw.api.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TableSawSegmentationTableModel implements AnnotationTableModel< TableSawSegmentRow >
{
	protected final String columnsPath;
	protected Collection< String > columnPaths;
	protected List< String > loadedColumnPaths;

	private HashMap< TableSawSegmentRow, Integer > rowToIndex;
	private HashMap< Integer, TableSawSegmentRow > indexToRow;
	private Table table;

	public TableSawSegmentationTableModel( String columnsPath )
	{
		this.columnsPath = columnsPath;
		rowToIndex = new HashMap<>();
		indexToRow = new HashMap<>();
	}

	@Override
	public int getNumRows()
	{
		return 0;
	}

	@Override
	public int getRowIndex( TableSawSegmentRow row )
	{
		return rowToIndex.get( row );
	}

	@Override
	public TableSawSegmentRow getRow( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TableSawSegmentRow row = new TableSawSegmentRow( table.row( rowIndex ) );
			rowToIndex.put( row, rowIndex );
			indexToRow.put( rowIndex, row );
		}

		return indexToRow.get( rowIndex );
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
	public Collection< String > getColumnPaths()
	{
		return columnPaths;
	}

	@Override
	public List< String > getLoadedColumnPaths()
	{
		return loadedColumnPaths;
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return null;
	}
}
