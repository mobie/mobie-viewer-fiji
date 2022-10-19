package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentTableModel implements AnnotationTableModel< AnnotatedSegment >
{
	private final String dataSourceName;
	private final List< String > columnNames;
	private final LinkedHashSet< String > loadedColumnPaths;
	private Map< AnnotatedSegment, Integer > annotationToRowIndex = new ConcurrentHashMap<>();
	private Map< Integer, AnnotatedSegment > rowIndexToAnnotation = new ConcurrentHashMap<>();
	private int numAnnotations = 0;
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
		return numAnnotations;
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

	}

	@Override
	public boolean isDataLoaded()
	{
		return true;
	}

	@Override
	public String dataStore()
	{
		return null;
	}

	@Override
	public synchronized void transform( AffineTransform3D affineTransform3D )
	{
	}

	public synchronized AnnotatedSegment createAnnotation( String source, int timePoint, int label )
	{
		final DefaultAnnotatedSegment annotatedSegment = new DefaultAnnotatedSegment( source, timePoint, label );
		rowIndexToAnnotation.put( numAnnotations, annotatedSegment );
		annotationToRowIndex.put( annotatedSegment, numAnnotations );
		numAnnotations++;
		return annotatedSegment;
	}
}
