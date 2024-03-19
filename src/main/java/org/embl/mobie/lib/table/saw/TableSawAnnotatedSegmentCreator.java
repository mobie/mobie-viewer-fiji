/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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

import net.imglib2.FinalRealInterval;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.MicrogliaSegmentColumnNames;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TableSawAnnotatedSegmentCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	private SegmentColumnNames segmentColumnNames;
	private int labelImageColumnIndex;
	private int labelIdColumnIndex;
	private int timePointColumnIndex;
	private int[] anchorColumnIndices;
	private int[] bbMinColumnIndices;
	private int[] bbMaxColumnIndices;

	private AtomicBoolean columnsInitialised = new AtomicBoolean( false );
	private boolean is3D;
	private boolean hasBoundingBox;
	private ArrayList< String > idColumns;

	public TableSawAnnotatedSegmentCreator( @Nullable Table table )
	{
		//this.segmentColumnNames = segmentColumnNames;
		if ( table != null )
			initColumns( table );
	}

	private synchronized void initColumns( Table table )
	{
		columnsInitialised.set( true );

		final List< String > columnNames = table.columnNames();

		segmentColumnNames = TableDataFormat.getSegmentColumnNames( columnNames );

		idColumns = new ArrayList<>();
		idColumns.add( segmentColumnNames.labelIdColumn() );
		idColumns.add( segmentColumnNames.timePointColumn() );
		idColumns.add( segmentColumnNames.labelImageColumn() );

		labelIdColumnIndex = columnNames.indexOf( segmentColumnNames.labelIdColumn() );
		if ( labelIdColumnIndex == -1 )
			throw new RuntimeException("The label id column \"" + segmentColumnNames.labelIdColumn() + "\" could not be found in table " + table.name() );

		timePointColumnIndex = columnNames.indexOf( segmentColumnNames.timePointColumn() );

		anchorColumnIndices = Arrays.stream( segmentColumnNames.anchorColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		bbMinColumnIndices = Arrays.stream( segmentColumnNames.bbMinColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		bbMaxColumnIndices = Arrays.stream( segmentColumnNames.bbMaxColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		labelImageColumnIndex = columnNames.indexOf( segmentColumnNames.labelImageColumn() );

		is3D = anchorColumnIndices.length == 3 && anchorColumnIndices[ 2 ] > -1;

		hasBoundingBox = bbMinColumnIndices[ 0 ] > -1;
	}

	@Override
	public TableSawAnnotatedSegment create( TableSawAnnotationTableModel< TableSawAnnotatedSegment > model, int rowIndex )
	{
		final Table table = model.getTable();

		if ( ! columnsInitialised.get() )
			initColumns( table );

		Integer labelId = table.intColumn( labelIdColumnIndex ).get( rowIndex );

		String source = labelImageColumnIndex > -1 ? table.stringColumn( labelImageColumnIndex ).get( rowIndex ) : table.name();

		int timePoint = timePointColumnIndex > -1 ? table.intColumn( timePointColumnIndex ).get( rowIndex ) : 0;

		if ( timePointColumnIndex > -1 && segmentColumnNames.timePointsAreOneBased()  )
			timePoint -= 1;

		final FinalRealInterval boundingBox = boundingBox( table, rowIndex );

		// TODO do we want to support missing anchor columns?
		double[] position = new double[]{
				table.numberColumn( anchorColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( anchorColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( anchorColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final String uuid = source + ";" + timePoint + ";" + labelId;

		return new TableSawAnnotatedSegment( model, rowIndex, source, uuid, labelId, timePoint, position, boundingBox );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}

	private FinalRealInterval boundingBox( Table table, int rowIndex )
	{
		if ( ! hasBoundingBox ) return null;

		// TODO: if we want to support this for IJ ParticleAnalyzer ResultsTable
		//  we need to do some math, because it is given as min and size.

		final double[] min = {
				table.numberColumn( bbMinColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( bbMinColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( bbMinColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final double[] max = {
				table.numberColumn( bbMaxColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( bbMaxColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( bbMaxColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final FinalRealInterval boundingBox = new FinalRealInterval( min, max );

		return boundingBox;
	}

	@Override
	public List< String > getIDColumns()
	{
		return idColumns;
	}
}
