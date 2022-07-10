package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedSegmentRow;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TransformedSegmentsTableModel implements SegmentsTableModel< TransformedSegmentRow >
{
	private final SegmentsTableModel< ? extends SegmentRow > model;
	private final AffineTransform3D transform3D;

	private HashMap< TransformedSegmentRow, Integer > rowToIndex;
	private HashMap< Integer, TransformedSegmentRow > indexToRow;

	public TransformedSegmentsTableModel( SegmentsTableModel< ? extends SegmentRow > model, AffineTransform3D transform3D )
	{
		this.model = model;
		this.transform3D = transform3D;
	}

	@Override
	public int getNumRows()
	{
		return model.getNumRows();
	}

	@Override
	public int getRowIndex( TransformedSegmentRow row )
	{
		return rowToIndex.get( row );
	}

	@Override
	public TransformedSegmentRow getRow( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TransformedSegmentRow row = new TransformedSegmentRow( model.getRow( rowIndex ), transform3D );
			rowToIndex.put( row, rowIndex );
			indexToRow.put( rowIndex, row );
		}

		return indexToRow.get( rowIndex );
	}

	@Override
	public void loadColumns( String columnsPath )
	{
		model.loadColumns( columnsPath );
	}

	@Override
	public void setColumnPaths( Collection< String > columnPaths )
	{
		model.setColumnPaths( columnPaths );
	}

	@Override
	public Collection< String > getColumnPaths()
	{
		return model.getColumnPaths();
	}

	@Override
	public List< String > getLoadedColumnPaths()
	{
		return model.getLoadedColumnPaths();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return null;
	}
}
