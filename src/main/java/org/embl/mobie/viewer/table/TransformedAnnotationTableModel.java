package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.transform.AnnotationTransformer;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class TransformedAnnotationTableModel< A extends Annotation, TA extends A > implements AnnotationTableModel< TA >
{
	private final AnnotationTableModel< A > annotationTableModel;
	private final AnnotationTransformer< A, TA > transformer;

	private HashMap< TA, Integer > rowToIndex;
	private HashMap< Integer, TA > indexToRow;

	public TransformedAnnotationTableModel( AnnotationTableModel< A > annotationTableModel, AnnotationTransformer< A, TA > transformer )
	{
		this.annotationTableModel = annotationTableModel;
		this.transformer = transformer;
		this.rowToIndex = new HashMap<>();
		this.indexToRow = new HashMap<>();
	}

	@Override
	public List< String > columnNames()
	{
		return annotationTableModel.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return annotationTableModel.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return annotationTableModel.columnClass( columnName );
	}

	@Override
	public int numRows()
	{
		return annotationTableModel.numRows();
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
			final TA row = transformer.transform( annotationTableModel.row( rowIndex ) );
			rowToIndex.put( row, rowIndex );
			indexToRow.put( rowIndex, row );
		}

		return indexToRow.get( rowIndex );
	}

	@Override
	public void loadColumns( String columnsPath )
	{
		annotationTableModel.loadColumns( columnsPath );
	}

	@Override
	public void setColumnPaths( Collection< String > availableColumnPaths )
	{
		annotationTableModel.setColumnPaths( availableColumnPaths );
	}

	@Override
	public Collection< String > columnPaths()
	{
		return annotationTableModel.columnPaths();
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return annotationTableModel.loadedColumnPaths();
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

	@Override
	public boolean isDataLoaded()
	{
		return annotationTableModel.isDataLoaded();
	}
}
