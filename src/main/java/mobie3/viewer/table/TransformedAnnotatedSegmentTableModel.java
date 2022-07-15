package mobie3.viewer.table;

import mobie3.viewer.annotation.AnnotatedSegment;
import mobie3.viewer.annotation.TransformedAnnotatedSegment;
import mobie3.viewer.transform.Transformation;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TransformedAnnotatedSegmentTableModel implements AnnotatedSegmentTableModel< TransformedAnnotatedSegment >
{
	private final AnnotatedSegmentTableModel< ? extends AnnotatedSegment > model;
	private final Transformation transformation;

	private HashMap< TransformedAnnotatedSegment, Integer > rowToIndex;
	private HashMap< Integer, TransformedAnnotatedSegment > indexToRow;

	public TransformedAnnotatedSegmentTableModel( AnnotatedSegmentTableModel< ? extends AnnotatedSegment > model, Transformation transformation )
	{
		this.model = model;
		this.transformation = transformation;
		this.rowToIndex = new HashMap<>();
		this.indexToRow = new HashMap<>();
	}

	@Override
	public List< String > columnNames()
	{
		return model.columnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return model.columnClass( columnName );
	}

	@Override
	public int numRows()
	{
		return model.numRows();
	}

	@Override
	public int indexOf( TransformedAnnotatedSegment annotation )
	{
		return rowToIndex.get( annotation );
	}

	@Override
	public TransformedAnnotatedSegment row( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TransformedAnnotatedSegment row = new TransformedAnnotatedSegment( model.row( rowIndex ), transformation );
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
	public Collection< String > columnPaths()
	{
		return model.columnPaths();
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return model.loadedColumnPaths();
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		return null;
	}

	@Override
	public Set< TransformedAnnotatedSegment > rows()
	{
		return rowToIndex.keySet();
	}
}
