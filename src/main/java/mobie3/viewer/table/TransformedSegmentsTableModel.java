package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedSegmentAnnotation;
import mobie3.viewer.transform.Transformation;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TransformedSegmentsTableModel implements SegmentsTableModel< TransformedSegmentAnnotation >
{
	private final SegmentsTableModel< ? extends SegmentAnnotation > model;
	private final Transformation transformation;

	private HashMap< TransformedSegmentAnnotation, Integer > rowToIndex;
	private HashMap< Integer, TransformedSegmentAnnotation > indexToRow;

	public TransformedSegmentsTableModel( SegmentsTableModel< ? extends SegmentAnnotation > model, Transformation transformation )
	{
		this.model = model;
		this.transformation = transformation;
		this.rowToIndex = new HashMap<>();
		this.indexToRow = new HashMap<>();
	}

	@Override
	public List< String > getColumnNames()
	{
		return model.getColumnNames();
	}

	@Override
	public Class< ? > getColumnClass( String columnName )
	{
		return model.getColumnClass( columnName );
	}

	@Override
	public int getNumRows()
	{
		return model.getNumRows();
	}

	@Override
	public int getRowIndex( TransformedSegmentAnnotation annotation )
	{
		return rowToIndex.get( annotation );
	}

	@Override
	public TransformedSegmentAnnotation getRow( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TransformedSegmentAnnotation row = new TransformedSegmentAnnotation( model.getRow( rowIndex ), transformation );
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
