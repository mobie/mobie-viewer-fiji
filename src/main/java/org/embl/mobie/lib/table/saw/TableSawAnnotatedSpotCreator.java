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

import org.embl.mobie.lib.table.columns.ColumnNames;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

public class TableSawAnnotatedSpotCreator implements TableSawAnnotationCreator< TableSawAnnotatedSpot >
{
	private final int spotIDColumnIndex;
	private final int xColumnIndex;
	private final int yColumnIndex;
	private final int zColumnIndex;
	private int timePointColumnIndex;
	private ArrayList< String > idColumns;

	public TableSawAnnotatedSpotCreator( Table table )
	{
		final List< String > columnNames = table.columnNames();
		spotIDColumnIndex = columnNames.indexOf( ColumnNames.SPOT_ID );
		xColumnIndex = columnNames.indexOf( ColumnNames.SPOT_X );
		yColumnIndex = columnNames.indexOf( ColumnNames.SPOT_Y );
		zColumnIndex = columnNames.indexOf( ColumnNames.SPOT_Z );
		timePointColumnIndex = columnNames.indexOf( ColumnNames.TIMEPOINT );
		idColumns = new ArrayList<>();
		idColumns.add( ColumnNames.SPOT_ID );
		idColumns.add( ColumnNames.TIMEPOINT );
	}

	@Override
	public TableSawAnnotatedSpot create( TableSawAnnotationTableModel< TableSawAnnotatedSpot > model, int rowIndex )
	{
		final Table table = model.getTable();
		final float[] position = new float[ 3 ];
		position[ 0 ] = ((Number) table.get( rowIndex, xColumnIndex )).floatValue();
		position[ 1 ] = ((Number) table.get( rowIndex, yColumnIndex )).floatValue();

		// FIXME kdTree issue: https://imagesc.zulipchat.com/#narrow/stream/327240-ImgLib2/topic/kdTree.20issue
		if ( zColumnIndex > -1 )
			position[ 2 ] = ((Number) table.get( rowIndex, zColumnIndex )).floatValue() + (float) ( 1e-3 * Math.random() );

		int label = ((Number) table.get( rowIndex, spotIDColumnIndex )).intValue();

		int timePoint = 0;
		if ( timePointColumnIndex > -1 )
			timePoint = ((Number) table.get( rowIndex, timePointColumnIndex )).intValue();

		String source = table.name();

		return new TableSawAnnotatedSpot( model, rowIndex, label, position, timePoint, source );
	}

	@Override
	public int[] removeColumns()
	{
		if ( zColumnIndex > -1 )
			return new int[]{ xColumnIndex, yColumnIndex, zColumnIndex };
		else
			return new int[]{ xColumnIndex, yColumnIndex };
	}

	@Override
	public List< String > getIDColumns()
	{
		return idColumns;
	}
}
