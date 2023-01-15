package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.io.StorageLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentTableModel extends AbstractAnnotationTableModel< AnnotatedSegment >
{
	private final String dataSourceName;
	private final List< String > columnNames;
	private final List< String > numericColumnNames;
	private final LinkedHashSet< String > loadedColumnPaths;
	private final ArrayList< AnnotatedSegment > annotations = new ArrayList<>();

	public LazyAnnotatedSegmentTableModel( String dataSourceName )
	{
		this.dataSourceName = dataSourceName;

		this.columnNames = DefaultAnnotatedSegment.columnToClass.keySet().stream().collect( Collectors.toList() );
		numericColumnNames = DefaultAnnotatedSegment.columnToClass.entrySet().stream().filter( entry -> entry.getValue().equals( Integer.class ) ).map( entry -> entry.getKey() ).collect( Collectors.toList() );

		loadedColumnPaths = new LinkedHashSet<>();
		loadedColumnPaths.add( "LazySegmentTable" );
	}

	@Override
	public List< String > columnNames()
	{
		return columnNames;
	}

	@Override
	public List< String > numericColumnNames()
	{
		return numericColumnNames;
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return DefaultAnnotatedSegment.columnToClass.get( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return annotations.size();
	}

	@Override
	public synchronized int rowIndexOf( AnnotatedSegment annotation )
	{
		return annotations.indexOf( annotation );
	}

	@Override
	public synchronized AnnotatedSegment annotation( int rowIndex )
	{
		return annotations.get( rowIndex );
	}

	@Override
	public void loadTableChunk( String tableChunk )
	{
		// not implemented
	}

	@Override
	public void setAvailableTableChunks( Set< String > columnPaths )
	{
		// not implemented
	}

	@Override
	public Collection< String > getAvailableTableChunks()
	{
		throw new UnsupportedOperationException( this.getClass().getName() + " does not support loading of additional tables." );
	}

	@Override
	public LinkedHashSet< String > getLoadedTableChunks()
	{
		return loadedColumnPaths;
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public synchronized ArrayList< AnnotatedSegment > annotations()
	{
		return annotations;
	}

	@Override
	public void addStringColumn( String columnName )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StorageLocation getStorageLocation()
	{
		return null;
	}

	@Override
	public synchronized void transform( AffineTransform3D affineTransform3D )
	{
		//throw new UnsupportedOperationException();
	}

	@Override
	public void addAnnotationListener( AnnotationListener< AnnotatedSegment > listener )
	{
		listeners.add( listener );
		if ( annotations.size() > 0 )
			listener.addAnnotations( annotations );
	}

	public AnnotatedSegment createAnnotation( String source, int timePoint, int label )
	{
		final DefaultAnnotatedSegment annotatedSegment = new DefaultAnnotatedSegment( source, timePoint, label );

		addAnnotation( annotatedSegment );

		return annotatedSegment;
	}

	@Override
	public void addAnnotations( Collection< AnnotatedSegment > annotations )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void addAnnotation( AnnotatedSegment annotation )
	{
		annotations.add( annotation );

		for ( AnnotationListener< AnnotatedSegment > listener : listeners.list )
			listener.addAnnotation( annotation );
	}
}
