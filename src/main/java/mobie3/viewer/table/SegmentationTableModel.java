package mobie3.viewer.table;

import net.imglib2.util.Pair;
import tech.tablesaw.api.Table;

import java.util.HashMap;
import java.util.List;

// currently TableSaw-based
public class SegmentationTableModel< R extends Row > implements AnnotationTableModel< R >
{
	private List< String > columnPaths;
	private List< String > loadedColumnPaths;
	private Table table;
	private int numDimensions;
	private HashMap< Row, Integer > rowToIndex;

	public SegmentationTableModel()
	{
		rowToIndex = new HashMap<>();
	}

	@Override
	public int getRowIndex( R row )
	{
		return rowToIndex.get( row );
	}

	@Override
	public R getRow( int rowIndex )
	{
		final TableSawSegmentRow row = new TableSawSegmentRow( table.row( rowIndex ) );
		rowToIndex.put( row, rowIndex );
		// TODO: INDEX TO ROW

		return rowToIndex.get( rowIndex );
	}

	@Override
	public void loadColumns( String columnsPath )
	{

	}

	@Override
	public List< String > getColumnPaths()
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
