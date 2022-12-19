package org.embl.mobie.viewer.table.ijresults;

import ij.measure.ResultsTable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AbstractAnnotationTableModel;
import org.embl.mobie.viewer.table.AnnotationListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ResultsTableAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A >
{
	public ResultsTableAnnotationTableModel( ResultsTable resultsTable )
	{

	}

	@Override
	public void addAnnotations( Collection< A > annotations )
	{

	}

	@Override
	public void addAnnotation( A annotation )
	{

	}

	@Override
	public List< String > columnNames()
	{
		return null;
	}

	@Override
	public List< String > numericColumnNames()
	{
		return null;
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return null;
	}

	@Override
	public int numAnnotations()
	{
		return 0;
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return 0;
	}

	@Override
	public A annotation( int rowIndex )
	{
		return null;
	}

	@Override
	public void requestAdditionalColumns( String columnsPath )
	{

	}

	@Override
	public void setTablePaths( Set< String > availableColumnPaths )
	{

	}

	@Override
	public Collection< String > getTablePaths()
	{
		return null;
	}

	@Override
	public LinkedHashSet< String > getAdditionalTablePaths()
	{
		return null;
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return null;
	}

	@Override
	public ArrayList< A > annotations()
	{
		return null;
	}

	@Override
	public void addStringColumn( String columnName )
	{

	}

	@Override
	public String dataStore()
	{
		return null;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{

	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{

	}
}
