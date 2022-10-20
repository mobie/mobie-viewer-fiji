package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.select.Listeners;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentTableModel extends AbstractAnnotationTableModel< AnnotatedSegment >
{
	private final String dataSourceName;
	private final List< String > columnNames;
	private final LinkedHashSet< String > loadedColumnPaths;
	private Map< AnnotatedSegment, Integer > annotationToRowIndex = new ConcurrentHashMap<>();
	private Map< Integer, AnnotatedSegment > rowIndexToAnnotation = new ConcurrentHashMap<>();
	private AtomicInteger numAnnotations = new AtomicInteger( 0 );
	private List< String > numericColumnNames;

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
		return numAnnotations.get();
	}

	@Override
	public synchronized int rowIndexOf( AnnotatedSegment annotation )
	{
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public synchronized AnnotatedSegment annotation( int rowIndex )
	{
		return rowIndexToAnnotation.get( rowIndex );
	}

	@Override
	public void requestColumns( String columnsPath )
	{
		// not implemented
	}

	@Override
	public void setAvailableColumnPaths( Set< String > columnPaths )
	{
		// not implemented
	}

	@Override
	public Collection< String > availableColumnPaths()
	{
		throw new UnsupportedOperationException( this.getClass().getName() + " does not support loading of additional tables." );
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return loadedColumnPaths;
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public synchronized Set< AnnotatedSegment > annotations()
	{
		return annotationToRowIndex.keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String dataStore()
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
		if ( numAnnotations.get() > 0 )
			listener.addAnnotations( annotations() );
	}

	public AnnotatedSegment createAnnotation( String source, int timePoint, int label )
	{
		final DefaultAnnotatedSegment annotatedSegment = new DefaultAnnotatedSegment( source, timePoint, label );

		addAnnotation( annotatedSegment );

		for ( AnnotationListener< AnnotatedSegment > listener : listeners.list )
			listener.addAnnotation( annotatedSegment );

		return annotatedSegment;
	}

	@Override
	public void addAnnotations( Collection< AnnotatedSegment > annotations )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAnnotation( AnnotatedSegment annotation )
	{
		final int rowIndex = numAnnotations.incrementAndGet() - 1;
		rowIndexToAnnotation.put( rowIndex, annotation );
		annotationToRowIndex.put( annotation, rowIndex );
	}
}
