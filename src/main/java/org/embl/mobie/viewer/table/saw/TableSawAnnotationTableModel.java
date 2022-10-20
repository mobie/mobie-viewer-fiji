package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AbstractAnnotationTableModel;
import org.embl.mobie.viewer.table.AnnotationListener;
import org.embl.mobie.viewer.table.DefaultValues;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TableSawAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A >
{
	private final String dataSourceName;
	private final TableSawAnnotationCreator< A > annotationCreator;
	private final String dataStore;
	private Set< String > availableColumnPaths;
	private LinkedHashSet< String > requestedColumnPaths = new LinkedHashSet<>();
	private LinkedHashSet< String > loadedTablePaths = new LinkedHashSet<>();
	private Map< A, Integer > annotationToRowIndex = new HashMap<>();
	private Map< Integer, A > rowIndexToAnnotation = new HashMap<>();
	private AtomicInteger numAnnotations = new AtomicInteger( 0 );

	private Table table;
	private AffineTransform3D affineTransform3D;
	private boolean updateTransforms = false;

	public TableSawAnnotationTableModel(
			String dataSourceName,
			TableSawAnnotationCreator< A > annotationCreator,
			String dataStore,
			String defaultTablePath )
	{
		this.dataSourceName = dataSourceName;
		this.annotationCreator = annotationCreator;
		this.dataStore = dataStore;
		this.requestedColumnPaths.add( IOHelper.combinePath( dataStore, defaultTablePath ) );
		this.affineTransform3D = new AffineTransform3D();
	}

	// Use this constructor if the default table is available already
	public TableSawAnnotationTableModel( String name, TableSawAnnotationCreator< A > annotationCreator, String tableStore, String defaultColumns, Table defaultTable )
	{
		this( name, annotationCreator, tableStore, defaultColumns );
		initTable( defaultTable );
		loadedTablePaths.add( requestedColumnPaths.iterator().next() );
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html

	private synchronized void update()
	{
		for ( String tablePath : requestedColumnPaths )
		{
			if ( loadedTablePaths.contains( tablePath ) )
				continue;

			loadedTablePaths.add( tablePath );

			// Note: Calling IJ.log inside here hangs for some reason,
			// maybe to do with the {@code synchronized} of this function.
			// IJ.log( "Opening table for " + dataSourceName + "..." );
			System.out.println( "TableModel: " + dataSourceName + ": Reading table:\n" + tablePath );
			final Table rows = TableSawHelper.readTable( tablePath, -1 );

			if ( table == null ) // init table
			{
				initTable( rows );
			}
			else // join additional table
			{
				final String[] mergeByColumnNames = annotation( 0 ).idColumns();
				table = table.joinOn( mergeByColumnNames ).inner( rows );
			}
		}

		if ( updateTransforms )
		{
			updateTransforms = false;
			for ( A annotation : annotationToRowIndex.keySet() )
				annotation.transform( affineTransform3D );
		}
	}

	private void initTable( Table rows )
	{
		table = rows;
		table.setName( dataSourceName );
		final int rowCount = table.rowCount();
		if ( ! table.containsColumn( "source" ) )
			table.addColumns( StringColumn.create( "source", rowCount ) );

		// TODO: Can we speed this up in any way?
		final long start = System.currentTimeMillis();
		final ArrayList< A > annotations = new ArrayList<>();
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
		{
			// https://github.com/jtablesaw/tablesaw/issues/1165
			table.row( rowIndex ).setText( "source", dataSourceName );
			annotations.add( annotationCreator.create( () -> table, rowIndex ) );
		}
		addAnnotations( annotations );

		System.out.println("Initialised " + rowCount + "table rows in " + (System.currentTimeMillis() - start ) + " ms.");
	}

	private Map< A, Integer > annotationToRowIndex()
	{
		update();
		return annotationToRowIndex;
	}

	private Map< Integer, A > rowIndexToAnnotation()
	{
		update();
		return rowIndexToAnnotation;
	}

	@Override
	public List< String > columnNames()
	{
		update();
		return table.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		update();
		return table.numericColumns().stream().map( c -> c.name() ).collect( Collectors.toList() );
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		update();
		final Class< ? > columnClass = TableSawColumnTypes.typeToClass.get( table.column( columnName ).type() );
		if ( columnClass == null )
			throw new RuntimeException("Could determine the class of column " + columnName );
		return columnClass;
	}

	@Override
	public int numAnnotations()
	{
		update();
		return numAnnotations.get();
	}

	@Override
	public synchronized int rowIndexOf( A annotation )
	{
		update();
		return annotationToRowIndex.get( annotation );
	}

	@Override
	public synchronized A annotation( int rowIndex )
	{
		final A annotation = rowIndexToAnnotation().get( rowIndex );

		if ( annotation == null ) // DEBUG
			throw new RuntimeException("TableSawAnnotationTableModel: RowIndex " + rowIndex + " does not exist.");

		return annotation;
	}

	@Override
	public void requestColumns( String columnsPath )
	{
		requestedColumnPaths.add( columnsPath );
	}

	@Override
	public void setAvailableColumnPaths( Set< String > columnPaths )
	{
		this.availableColumnPaths = columnPaths;
	}

	@Override
	public Collection< String > availableColumnPaths()
	{
		if ( availableColumnPaths == null )
		{
			final String parentLocation = IOHelper.getParentLocation( requestedColumnPaths.iterator().next() );
			availableColumnPaths = Arrays.stream( IOHelper.getFileNames( parentLocation ) ).collect( Collectors.toSet() );
		}
		return availableColumnPaths;
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return requestedColumnPaths;
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public synchronized Set< A > annotations()
	{
		return annotationToRowIndex().keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{
		update();

		if ( ! table.containsColumn( columnName ) )
		{
			final String[] strings = new String[ table.rowCount() ];
			Arrays.fill( strings, DefaultValues.NONE );
			final StringColumn stringColumn = StringColumn.create( columnName, strings );
			table.addColumns( stringColumn );
		}
		else
		{
			throw new UnsupportedOperationException("Column " + columnName + " exists already.");
		}
	}

	@Override
	public String dataStore()
	{
		return dataStore;
	}

	@Override
	public synchronized void transform( AffineTransform3D affineTransform3D )
	{
		this.updateTransforms = true;
		this.affineTransform3D = affineTransform3D;
	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{
		listeners.add( listener );
		if ( table != null )
			listener.addAnnotations( annotations() );
	}

	@Override
	public void addAnnotations( Collection< A > annotations )
	{
		for( A annotation : annotations )
			addAnnotation( annotation );
	}

	@Override
	public void addAnnotation( A annotation )
	{
		final int rowIndex = numAnnotations.incrementAndGet() - 1;
		annotationToRowIndex.put( annotation, rowIndex );
		rowIndexToAnnotation.put( rowIndex, annotation );
	}
}
