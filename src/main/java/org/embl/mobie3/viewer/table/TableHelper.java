/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie3.viewer.table;

import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.tablerow.TableRow;
import org.embl.mobie3.viewer.MoBIEHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableHelper
{
	public static Map< String, List< String > > loadTableAndAddImageIdColumn( String imageID, String tablePath )
	{
		Logger.log( "Opening table: " + tablePath );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		TableHelper.addColumn( columns, ColumnNames.LABEL_IMAGE_ID, imageID );
		return columns;
	}

	public static Map< String, List< String > > addColumn(
			Map< String, List< String > > columns,
			String columnName,
			String value )
	{
		final int numRows = columns.values().iterator().next().size();
		final List< String > values = new ArrayList<>();
		for ( int row = 0; row < numRows; row++ )
			values.add( value );
		columns.put( columnName, values );
		return columns;
	}

	public static Map< String, List< String > > createColumnsForMerging( List< ? extends TableRow > segments, Map< String, List< String > > newColumns )
	{
		final ArrayList< String > segmentIdColumn = TableColumns.getColumn( segments, ColumnNames.LABEL_ID );
		final ArrayList< String > imageIdColumn = TableColumns.getColumn( segments, ColumnNames.LABEL_IMAGE_ID );
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		referenceColumns.put( ColumnNames.LABEL_IMAGE_ID, imageIdColumn );
		referenceColumns.put( ColumnNames.LABEL_ID, segmentIdColumn );

		// deal with the fact that the label ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( segmentIdColumn );
		MoBIEHelper.toDoubleStrings( newColumns.get( ColumnNames.LABEL_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, newColumns );

		return columnsForMerging;
	}
}
