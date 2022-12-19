package org.embl.mobie.viewer.table.saw;

import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AbstractAnnotationTableModel;
import org.embl.mobie.viewer.table.AnnotationListener;
import org.embl.mobie.viewer.table.DefaultValues;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSawAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel<A>
{
	private final String dataSourceName;
	private final TableSawAnnotationCreator< A > annotationCreator;
	private final String dataStore;
	private Set< String > tablePaths;
	private LinkedHashSet< String > additionalTablePaths = new LinkedHashSet<>();
	private LinkedHashSet< String > loadedTablePaths = new LinkedHashSet<>();
	private ArrayList< A > annotations = new ArrayList<>();

	private Table table;
	private AffineTransform3D affineTransform3D = new AffineTransform3D();
	private boolean updateTransforms = false;
	private String defaultTablePath;

	public TableSawAnnotationTableModel(
			String dataSourceName,
			TableSawAnnotationCreator< A > annotationCreator,
			String dataStore,
			String defaultTableLocation )
	{
		this.dataSourceName = dataSourceName;
		this.annotationCreator = annotationCreator;
		this.dataStore = dataStore;
		this.defaultTablePath = IOHelper.combinePath( dataStore, defaultTableLocation );
	}

	// use this if the default table is available already
	public TableSawAnnotationTableModel(
			String name,
			TableSawAnnotationCreator< A > annotationCreator,
			String tableStore,
			String defaultTableLocation,
			Table defaultTable )
	{
		this( name, annotationCreator, tableStore, defaultTableLocation );
		initTable( defaultTable );
		loadedTablePaths.add( defaultTablePath );
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	// https://jtablesaw.github.io/tablesaw/userguide/tables.html

	private synchronized void update()
	{
		if ( table == null )
			initTable( readTable( defaultTablePath ) );

		final List< String > tablePaths = additionalTablePaths.stream()
				.filter( path -> ! loadedTablePaths.contains( path ) )
				.collect( Collectors.toList() );

		for ( String tablePath : tablePaths )
			joinTable( readTable( tablePath ) );

		synchronized ( affineTransform3D )
		{
			if ( updateTransforms )
			{
				//System.out.println( "Table Model " + MoBIEHelper.getFileName( dataStore ) + ": applying " + affineTransform3D );
				for ( A annotation : annotations )
					annotation.transform( affineTransform3D );
				updateTransforms = false;
				// reset the transform as it has been applied
				affineTransform3D = new AffineTransform3D();
			}
		}
	}

	private Table readTable( String tablePath )
	{
		loadedTablePaths.add( tablePath );
		return TableSawHelper.readTable( tablePath, -1 );
	}

	private void joinTable( Table additionalTable )
	{
		// join additional table
		// some columns, e.g. timepoint, are optional and thus
		// are only used for merging if they are actually present
		final List< String > columnNames = table.columnNames();
		final List< String > mergeByColumnNames = Arrays.stream( annotation( 0 ).idColumns() ).filter( column -> columnNames.contains( column ) ).collect( Collectors.toList() );

		// note that the below joining changes the table object,
		// thus other classes that need that table object
		// need to retrieve the new one using the {@code getTable()}
		// method
		try
		{
			final List< String > additionalColumnNames = additionalTable.columnNames().stream().filter( col -> ! mergeByColumnNames.contains( col ) ).collect( Collectors.toList() );
			final List< String > duplicateColumnNames = additionalColumnNames.stream().filter( col -> columnNames.contains( col ) ).collect( Collectors.toList() );
			if ( duplicateColumnNames.size() > 0 )
			{
				final String[] duplicateColumnsArray = duplicateColumnNames.toArray( new String[ 0 ] );
				IJ.log( "There are duplicate columns: " + Arrays.toString( duplicateColumnsArray ) );
				IJ.log( "Those columns will be replaced by the columns in the newly loaded table." );
				table.removeColumns( duplicateColumnsArray );
			}
			table = table.joinOn( mergeByColumnNames.toArray( new String[ 0 ] ) ).leftOuter( additionalTable  );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private void initTable( Table rows )
	{
		table = rows;
		table.setName( dataSourceName );
		final int rowCount = table.rowCount();
		if ( ! table.containsColumn( "source" ) )
		{
			final String[] strings = new String[ table.rowCount() ];
			Arrays.fill( strings, dataSourceName );
			final StringColumn source = StringColumn.create( "source", strings );
			table.addColumns( source );
		}

		final ArrayList< A > annotations = new ArrayList<>( rowCount );
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
			annotations.add( annotationCreator.create( this, rowIndex ) );

		// FIXME:
		//  - refer to the removed columns via the annotations
		//  - ensure that {@code .removeColumns()} is properly implemented
		//    for all {@code AnnotationCreator}
		table.removeColumns( annotationCreator.removeColumns() );

		addAnnotations( annotations );
	}

	public Table getTable()
	{
		return table;
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
	public synchronized Class< ? > columnClass( String columnName )
	{
		update();
		final ColumnType type = table.column( columnName ).type();
		final Class< ? > columnClass = TableSawColumnTypes.typeToClass.get( type );
		if ( columnClass == null )
			throw new RuntimeException("Could determine the class of column " + columnName );
		return columnClass;
	}

	@Override
	public int numAnnotations()
	{
		update();

		return annotations.size();
	}

	@Override
	public synchronized int rowIndexOf( A annotation )
	{
		update();

		// TODO Note that a Map may be more efficient, but
		//   since this method is not called very frequently
		//   the current implementation may do and avoid building the map,
		//   which can be substantial for millions of elements such as
		//   in the case of spatial-omics data.
		//   But I am not 100% sure here...
		//   One may also consider out-sourcing the indexOf to the
		//   classes that actually need this. Maybe they could maintain a
		//   map, if needed. This would also avoid having this method here
		//   at all.

		return annotations.indexOf( annotation );
	}

	@Override
	public synchronized A annotation( int rowIndex )
	{
		update();

		try
		{
			final A annotation = annotations.get( rowIndex );
			return annotation;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	@Override
	public void requestAdditionalColumns( String tablePath )
	{
		additionalTablePaths.add( tablePath );
	}

	@Override
	public void setTablePaths( Set< String > tablePaths )
	{
		this.tablePaths = tablePaths;
	}

	@Override
	public Collection< String > getTablePaths()
	{
		if ( tablePaths == null )
		{
			tablePaths = Arrays.stream( IOHelper.getFileNames( dataStore ) ).map( fileName -> IOHelper.combinePath( dataStore, fileName ) ).collect( Collectors.toSet() );
		}
		return tablePaths;
	}

	@Override
	public LinkedHashSet< String > getAdditionalTablePaths()
	{
		return additionalTablePaths; // excluding the default table
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public synchronized ArrayList< A > annotations()
	{
		update();

		return annotations;
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		synchronized ( this.affineTransform3D )
		{
			//System.out.println( "Table Model " + MoBIEHelper.getFileName( dataStore ) + ": adding " + affineTransform3D );
			this.updateTransforms = true;
			this.affineTransform3D.preConcatenate( affineTransform3D );
		}
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
	public synchronized void addAnnotation( A annotation )
	{
		annotations.add( annotation );

		for ( AnnotationListener< A > listener : listeners.list )
			listener.addAnnotation( annotation );
	}
}
