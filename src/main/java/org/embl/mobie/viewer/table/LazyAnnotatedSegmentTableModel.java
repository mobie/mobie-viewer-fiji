package org.embl.mobie.viewer.table;

import net.imglib2.ops.parse.token.Int;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AnnotationTableModel;
import org.embl.mobie.viewer.table.DefaultValues;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.viewer.table.saw.TableSawColumnTypes;
import org.embl.mobie.viewer.table.saw.TableSawHelper;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LazyAnnotatedSegmentTableModel implements AnnotationTableModel< AnnotatedSegment >
{
	private final String dataSourceName;
	private final ArrayList< String > columnNames;
	private final HashSet< String > loadedColumnPaths;
	private Map< AnnotatedSegment, Integer > annotationToRowIndex = new ConcurrentHashMap<>();
	private Map< Integer, AnnotatedSegment > rowIndexToAnnotation = new ConcurrentHashMap<>();
	private int numAnnotations = 0;

	public LazyAnnotatedSegmentTableModel( String dataSourceName )
	{
		this.dataSourceName = dataSourceName;
		this.columnNames = new ArrayList< String >();
		columnNames.add( ColumnNames.LABEL_ID );
		loadedColumnPaths = new HashSet< String >();
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
		return columnNames;
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return Integer.class; // label_id
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
		throw new UnsupportedOperationException( this.getClass().getName() + " does not support loading of additional tables." );
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
		throw new UnsupportedOperationException( this.getClass().getName() + " cannot be annotated.");
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

	public AnnotatedSegment createAnnotation( String source, int timePoint, int label )
	{
		final DefaultAnnotatedSegment annotatedSegment = new DefaultAnnotatedSegment( source, timePoint, label );
		rowIndexToAnnotation.put( numAnnotations, annotatedSegment );
		numAnnotations++;
		return annotatedSegment;
	}
}
