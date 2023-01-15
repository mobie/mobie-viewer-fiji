package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.io.StorageLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConcatenatedAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A >
{
	private final Set< AnnotationTableModel< A > > tableModels;
	private AnnotationTableModel< A > referenceTable;
	private ArrayList< A > annotations = new ArrayList<>();

	public ConcatenatedAnnotationTableModel( Set< AnnotationTableModel< A > > tableModels )
	{
		this.tableModels = tableModels;

		// Note that all loading of data from the {@code tableModels}
		// it handled by the listening
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.addAnnotationListener( this );

		this.referenceTable = tableModels.iterator().next();
	}

	@Override
	public List< String > columnNames()
	{
		return referenceTable.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return referenceTable.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return referenceTable.columnClass( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return annotations.size();
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return annotations.indexOf( annotation );
	}

	@Override
	public A annotation( int rowIndex )
	{
		// We do not update the tables here,
		// because one should only ask for
		// rows with an index lower than the
		// current numRows.
		return annotations.get( rowIndex );
	}

	@Override
	public void loadTableChunk( String tableChunk )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.loadTableChunk( tableChunk );
	}

//	@Override
//	public void setAvailableTableChunks( Set< String > availableTableChunks )
//	{
//		for ( AnnotationTableModel< A > tableModel : tableModels )
//			tableModel.setAvailableTableChunks( availableTableChunks );
//	}

	@Override
	public Collection< String > getAvailableTableChunks()
	{
		return referenceTable.getAvailableTableChunks();
	}

	@Override
	public LinkedHashSet< String > getLoadedTableChunks()
	{
		return referenceTable.getLoadedTableChunks();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public ArrayList< A > annotations()
	{
		return annotations;
	}

	@Override
	public void addStringColumn( String columnName )
	{
		// here we probably need to load all tables
		throw new UnsupportedOperationException("Annotation of concatenated tables is not yet implemented.");
	}

	@Override
	public StorageLocation getStorageLocation()
	{
		return referenceTable.getStorageLocation();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.transform( affineTransform3D );
	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{
		listeners.add( listener );
		if( annotations.size() > 0 )
			listener.addAnnotations( annotations );
	}

	@Override
	public void addAnnotations( Collection< A > annotations )
	{
		for( A annotation : annotations )
			addAnnotation( annotation );
	}

	@Override
	public synchronized void addAnnotation( A annotation )
	{
		annotations.add( annotation );

		for ( AnnotationListener< A > annotationListener : listeners.list )
			annotationListener.addAnnotation( annotation );
	}
}
