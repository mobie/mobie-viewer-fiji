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
	private final AnnotationTableModel< A > tableModel;
	private final AnnotationTransformer< A, TA > transformer;

	private HashMap< TA, Integer > rowToIndex;
	private HashMap< Integer, TA > indexToRow;

	public TransformedAnnotationTableModel( AnnotationTableModel< A > tableModel, AnnotationTransformer< A, TA > transformer )
	{
		this.tableModel = tableModel;
		this.transformer = transformer;
		this.rowToIndex = new HashMap<>();
		this.indexToRow = new HashMap<>();
	}

	@Override
	public List< String > columnNames()
	{
		return tableModel.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return tableModel.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return tableModel.columnClass( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return tableModel.numAnnotations();
	}

	@Override
	public int rowIndexOf( TA annotation )
	{
		return 0;
	}

	@Override
	public TA annotation( int rowIndex )
	{
		if ( ! indexToRow.containsKey( rowIndex ) )
		{
			final TA row = transformer.transform( tableModel.annotation( rowIndex ) );
			rowToIndex.put( row, rowIndex );
			indexToRow.put( rowIndex, row );
		}

		return indexToRow.get( rowIndex );
	}

	@Override
	public void requestColumns( String columnsPath )
	{
		tableModel.requestColumns( columnsPath );
	}

	@Override
	public void setAvailableColumnPaths( Set< String > availableColumnPaths )
	{
		tableModel.setAvailableColumnPaths( availableColumnPaths );
	}

	@Override
	public Collection< String > availableColumnPaths()
	{
		return tableModel.availableColumnPaths();
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return tableModel.loadedColumnPaths();
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		return null;
	}

	@Override
	public Set< TA > annotations()
	{
		// FIXME This must trigger initialisation of rowToIndex
		throw new RuntimeException( "FIXME: transformed annotation table model" );
		//return rowToIndex.keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{

	}

	@Override
	public boolean isDataLoaded()
	{
		return tableModel.isDataLoaded();
	}

	@Override
	public String dataStore()
	{
		return tableModel.dataStore();
	}
}
