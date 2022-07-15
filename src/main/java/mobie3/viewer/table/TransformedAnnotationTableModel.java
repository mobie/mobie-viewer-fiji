package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.transform.AnnotationTransformer;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class TransformedAnnotationTableModel< A extends Annotation, TA extends A > implements AnnotationTableModel< TA >
{
	private final AnnotationTableModel< A > model;
	private final AnnotationTransformer< A, TA > transformer;

	private HashMap< TA, Integer > rowToIndex;
	private HashMap< Integer, TA > indexToRow;

	public TransformedAnnotationTableModel( AnnotationTableModel< A > model, AnnotationTransformer< A, TA > transformer )
	{
		this.model = model;
		this.transformer = transformer;
		this.rowToIndex = new HashMap<>();
		this.indexToRow = new HashMap<>();
	}

	@Override
	public List< String > columnNames()
	{
		return model.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return model.numericColumnNames();
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
	public int indexOf( TA annotation )
	{
		return 0;
	}

	@Override
	public TA row( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TA row = transformer.transform( model.row( rowIndex ) );
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
	public Set< TA > rows()
	{
		return rowToIndex.keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{

	}
}
