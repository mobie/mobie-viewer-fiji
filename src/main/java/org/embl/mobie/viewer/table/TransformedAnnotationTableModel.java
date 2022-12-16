package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.transform.AnnotationTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TransformedAnnotationTableModel< A extends Annotation, TA extends A > extends AbstractAnnotationTableModel< TA >
{
	private final AnnotationTableModel< A > tableModel;
	private final AnnotationTransformer< A, TA > transformer;
	private ArrayList< TA > annotations;

	public TransformedAnnotationTableModel( AnnotationTableModel< A > tableModel, AnnotationTransformer< A, TA > transformer )
	{
		this.tableModel = tableModel;
		this.transformer = transformer;
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
		update();
		return annotations.indexOf( annotation );
	}

	@Override
	public TA annotation( int rowIndex )
	{
		update();
		return annotations.get( rowIndex );
	}

	@Override
	public void requestAdditionalColumns( String columnsPath )
	{
		tableModel.requestAdditionalColumns( columnsPath );
	}

	@Override
	public void setTablePaths( Set< String > availableColumnPaths )
	{
		tableModel.setTablePaths( availableColumnPaths );
	}

	@Override
	public Collection< String > getTablePaths()
	{
		return tableModel.getTablePaths();
	}

	@Override
	public LinkedHashSet< String > getAdditionalTablePaths()
	{
		return tableModel.getAdditionalTablePaths();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public ArrayList< TA > annotations()
	{
		update();
		return annotations;
	}

	private synchronized void update()
	{
		if ( annotations == null )
		{
			annotations = new ArrayList<>();

			final int numAnnotations = tableModel.numAnnotations();
			for ( int rowIndex = 0; rowIndex < numAnnotations; rowIndex++ )
			{
				final TA transformedAnnotation = transformer.transform( tableModel.annotation( rowIndex ) );
				annotations.add( transformedAnnotation );
			}
		}
	}

	@Override
	public void addStringColumn( String columnName )
	{
		tableModel.addStringColumn( columnName );
	}

	@Override
	public String dataStore()
	{
		return tableModel.dataStore();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		throw new RuntimeException("Transforming a TransformedAnnotationTableModel is not yet implemented.");
	}

	@Override
	public void addAnnotationListener( AnnotationListener< TA > listener )
	{
		listeners.add( listener );
	}

	@Override
	public void addAnnotations( Collection< TA > annotations )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAnnotation( TA annotation )
	{
		throw new UnsupportedOperationException();
	}
}
