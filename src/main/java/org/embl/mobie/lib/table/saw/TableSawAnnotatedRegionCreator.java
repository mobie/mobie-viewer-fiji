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

import org.embl.mobie.lib.table.ColumnNames;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Map;

public class TableSawAnnotatedRegionCreator implements TableSawAnnotationCreator< TableSawAnnotatedRegion >
{
	private final Map< String, List< String > > regionIdToImageNames;
	private final int timePointColumnIndex;
	private final double relativeDilation;
	private int regionIdColumnIndex;

	public TableSawAnnotatedRegionCreator( Table table, Map< String, List< String > > regionIdToImageNames, double relativeDilation )
	{
		this.relativeDilation = relativeDilation;
		final List< String > columnNames = table.columnNames();
		this.regionIdToImageNames = regionIdToImageNames;
		regionIdColumnIndex = columnNames.indexOf( ColumnNames.REGION_ID );
		timePointColumnIndex = columnNames.indexOf( ColumnNames.TIMEPOINT );
	}

	@Override
	public TableSawAnnotatedRegion create( TableSawAnnotationTableModel< TableSawAnnotatedRegion > model, int rowIndex )
	{
		final Table table = model.getTable();

		final String regionId = table.getString( rowIndex, regionIdColumnIndex );

		final int labelId = rowIndex + 1; // 0 is the background label, thus we add 1

		Integer timePoint = timePointColumnIndex > -1 ? ( int ) table.get( rowIndex, timePointColumnIndex ) : null;

		final String uuid = timePoint + ";" + regionId;

		return new TableSawAnnotatedRegion( model, rowIndex, regionIdToImageNames.get( regionId ), timePoint, regionId, labelId, uuid, relativeDilation );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}
}
