/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.table.saw;

import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.io.Status;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.AbstractAnnotationTableModel;
import org.embl.mobie.lib.table.AnnotationListener;
import org.embl.mobie.lib.table.DefaultValues;
import org.embl.mobie.lib.table.TableDataFormat;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// https://jtablesaw.github.io/tablesaw/userguide/tables.html
public class TableSawAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A >
{
	private final String dataSourceName;
	private final TableSawAnnotationCreator< A > annotationCreator;
	private ArrayList< A > annotations = new ArrayList<>();

	private Set< String > availableTableChunks;
	private LinkedHashMap< String, Status > chunkToStatus = new LinkedHashMap<>();
	private LinkedHashMap< StorageLocation, Status > externalChunkToStatus = new LinkedHashMap<>();

	private Table table;
	private AffineTransform3D affineTransform3D = new AffineTransform3D();
	private boolean updateTransforms = false;
	private final StorageLocation storageLocation;
	private final TableDataFormat tableDataFormat;

	public TableSawAnnotationTableModel(
			String name,
			TableSawAnnotationCreator< A > annotationCreator,
			@Nullable StorageLocation storageLocation, // needed to load additional table chunks
			@Nullable TableDataFormat tableDataFormat, // needed to load additional table chunks
			@Nullable Table defaultTable )
	{
		this.dataSourceName = name;
		this.annotationCreator = annotationCreator;
		this.storageLocation = storageLocation;
		this.tableDataFormat = tableDataFormat;

		if ( defaultTable != null )
		{
			initTable( defaultTable );
		}
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	private synchronized void update()
	{
		if ( table == null )
		{
			initTable( openTableChunk( storageLocation.defaultChunk ) );
		}

		// load and join internal table chunks
		//
		boolean columnsAdded = false;

		final List< String > tableChunks = chunkToStatus.entrySet().stream()
				.filter( chunk -> chunk.getValue().equals( Status.Closed ) )
				.map( chunk -> chunk.getKey() )
				.collect( Collectors.toList() );

		for ( String tableChunk : tableChunks )
		{
			joinTable( openTableChunk( tableChunk ) );
			columnsAdded = true;
		}

		// load and join external table chunks
		//
		final List< StorageLocation > storageLocations = externalChunkToStatus
				.entrySet().stream()
				.filter( chunk -> chunk.getValue().equals( Status.Closed ) )
				.map( chunk -> chunk.getKey() )
				.collect( Collectors.toList() );

		for ( StorageLocation storageLocation : storageLocations )
		{
			joinTable( openExternalTableChunk( storageLocation ) );
			columnsAdded = true;
		}

		if ( columnsAdded )
		{
			for ( AnnotationListener< A > listener : listeners.list )
				listener.columnsAdded( null );
		}

		synchronized ( affineTransform3D )
		{
			if ( updateTransforms )
			{
				//System.out.println( "Table Model " + IOHelper.getFileName( dataStore ) + ": applying " + affineTransform3D );
				for ( A annotation : annotations )
					annotation.transform( affineTransform3D );
				updateTransforms = false;
				// reset the transform as it has been applied
				affineTransform3D = new AffineTransform3D();
			}
		}
	}

	private Table openTableChunk( String tableChunk )
	{
		chunkToStatus.put( tableChunk, Status.Opening );
		final Table table = TableOpener.open( storageLocation, tableChunk, tableDataFormat );
		chunkToStatus.put( tableChunk, Status.Open );
		return table;
	}

	private Table openExternalTableChunk( StorageLocation storageLocation )
	{
		externalChunkToStatus.put( storageLocation, Status.Opening );
		final String chunk = storageLocation.defaultChunk;
		final TableDataFormat format = TableDataFormat.fromPath( chunk );
		final Table table = TableOpener.open( storageLocation, chunk, format );
		externalChunkToStatus.put( storageLocation, Status.Open );
		return table;
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

		annotations = new ArrayList<>( rowCount );
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
			annotations.add( annotationCreator.create( this, rowIndex ) );

		for ( AnnotationListener< A > listener : listeners.list )
			listener.annotationsAdded( annotations );

		// Some columns are needed to create the annotations,
		// but then we don't want to keep them, to save
		// memory and also because it is not interesting to
		// view them in the table.
		// Currently, this only concerns the SpotAnnotations.
		table.removeColumns( annotationCreator.removeColumns() );
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

		// TODO a Map may be more efficient, but
		//   since this method is not called very frequently
		//   the current implementation may do, and avoid building the map,
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
		return  annotations.get( rowIndex );
	}

	@Override
	public void loadTableChunk( String tableChunk )
	{
		chunkToStatus.put( tableChunk, Status.Closed );
	}

	@Override
	public void loadExternalTableChunk( StorageLocation location )
	{
		externalChunkToStatus.put( location, Status.Closed );
	}

	@Override
	public Collection< String > getAvailableTableChunks()
	{
		if ( availableTableChunks == null )
			availableTableChunks = Arrays.stream( IOHelper.getFileNames( storageLocation.absolutePath ) ).collect( Collectors.toSet() );

		return availableTableChunks;
	}

	@Override
	public LinkedHashSet< String > getLoadedTableChunks()
	{
		return new LinkedHashSet<>( chunkToStatus.keySet() );
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

		if ( table.containsColumn( columnName ) )
			throw new UnsupportedOperationException("Column " + columnName + " exists already.");

		final String[] strings = new String[ table.rowCount() ];
		Arrays.fill( strings, DefaultValues.NONE );
		final StringColumn stringColumn = StringColumn.create( columnName, strings );
		table.addColumns( stringColumn );

		for ( AnnotationListener< A > listener : listeners.list )
			listener.columnsAdded( Collections.singleton( columnName ) );
	}

	@Override
	public StorageLocation getStorageLocation()
	{
		return storageLocation;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		synchronized ( this.affineTransform3D )
		{
			//System.out.println( "Table Model " + IOHelper.getFileName( dataStore ) + ": adding " + affineTransform3D );
			this.updateTransforms = true;
			this.affineTransform3D.preConcatenate( affineTransform3D );
		}
	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{
		listeners.add( listener );
		if ( table != null )
			listener.annotationsAdded( annotations() );
	}
}
