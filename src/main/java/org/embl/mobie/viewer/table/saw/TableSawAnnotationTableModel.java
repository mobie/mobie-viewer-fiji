package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AnnotationTableModel;
import org.embl.mobie.viewer.table.DefaultValues;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TableSawAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	private final String dataSourceName;
	private final TableSawAnnotationCreator< A > annotationCreator;
	private final String dataStore;
	protected Set< String > availableColumnPaths;
	protected LinkedHashSet< String > requestedColumnPaths = new LinkedHashSet<>();
	protected LinkedHashSet< String > loadedColumnPaths = new LinkedHashSet<>();

	private Map< A, Integer > annotationToRowIndex = new ConcurrentHashMap<>();;
	private Map< Integer, A > rowIndexToAnnotation = new ConcurrentHashMap<>();;
	private Table table;
	private AffineTransform3D affineTransform3D;
	private boolean updateTransforms = false;

	public TableSawAnnotationTableModel(
			String dataSourceName,
			TableSawAnnotationCreator< A > annotationCreator,
			String dataStore,
			String defaultColumns
	)
	{
		this.dataSourceName = dataSourceName;
		this.annotationCreator = annotationCreator;
		this.dataStore = dataStore;
		this.requestedColumnPaths.add( IOHelper.combinePath( dataStore, defaultColumns ) );
		this.affineTransform3D = new AffineTransform3D();
	}

	// Use this constructor if the default table is available already
	public TableSawAnnotationTableModel( String name, TableSawAnnotationCreator< A > annotationCreator, String tableStore, String defaultColumns, Table defaultTable )
	{
		this( name, annotationCreator, tableStore, defaultColumns );
		initTable( defaultTable );
		loadedColumnPaths.add( requestedColumnPaths.iterator().next() );
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html

	private synchronized void update()
	{
		for ( String columnPath : requestedColumnPaths )
		{
			if ( loadedColumnPaths.contains( columnPath ) )
				continue;

			loadedColumnPaths.add( columnPath );

			// Calling IJ.log inside here hangs for some reason,
			// maybe to do with the {@code synchronized} of this function.
			// IJ.log( "Opening table for " + dataSourceName + "..." );
			System.out.println( "TableModel: " + dataSourceName + ": Reading table:\n" + columnPath );
			final Table rows = TableSawHelper.readTable( columnPath );

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
			{
				if ( annotation instanceof AnnotatedRegion )
				{
					// FIXME: in this case we may not transform at all!
					//   because an annotated region obtains
					//   the spatial coordinates from the regions.
					int a = 1;
				}
				annotation.transform( affineTransform3D );
			}
		}
	}

	private void initTable( Table rows )
	{
		table = rows;
		table.setName( dataSourceName );
		final int rowCount = table.rowCount();
		// table.reorderColumns(  ) TODO MAYBE move the source in front
		table.addColumns( StringColumn.create( "source", rowCount ) );
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
		{
			final A annotation = annotationCreator.create( () -> table, rowIndex );
			annotationToRowIndex.put( annotation, rowIndex );
			rowIndexToAnnotation.put( rowIndex, annotation );
			table.row( rowIndex ).setText( "source", dataSourceName );
		}
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
		return TableSawColumnTypes.typeToClass.get( table.column( columnName ).type() );
	}

	@Override
	public int numAnnotations()
	{
		update();
		final int rowCount = table.rowCount();
		return rowCount;
	}

	@Override
	public synchronized int rowIndexOf( A annotation )
	{
		return annotationToRowIndex().get( annotation );
	}

	@Override
	public synchronized A annotation( int rowIndex )
	{
		final A annotation = rowIndexToAnnotation().get( rowIndex );
		if ( annotation == null )
		{
			int a = 1; // FIXME: Serr
		}
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
		// one could consider caching the results...
		final NumericColumn< ? > numericColumn = table.nCol( columnName );
		final Summarizer summarize = table.summarize( numericColumn, AggregateFunctions.min, AggregateFunctions.max );
		final Table summary = summarize.apply();
		final ValuePair< Double, Double > minMax = new ValuePair( summary.get( 0, 0 ), summary.get( 0, 1  ) );
		return minMax;
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
	public boolean isDataLoaded()
	{
		return table != null;
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
}
