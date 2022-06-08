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
package org.embl.mobie.viewer.table;

import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.annotate.RegionTableRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableHelper
{
	public static void appendRegionTableColumns( List< RegionTableRow > tableRows, Map< String, List< String > > columns )
	{
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		final ArrayList< String > regionIdColumn = TableColumns.getColumn( tableRows, TableColumnNames.REGION_ID );
		referenceColumns.put( TableColumnNames.REGION_ID, regionIdColumn );

		// deal with the fact that the grid ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( regionIdColumn );
		MoBIEHelper.toDoubleStrings( columns.get( TableColumnNames.REGION_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, columns );

		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	public static Map< String, List< String > > loadTableAndAddImageIdColumn( String imageID, String tablePath )
	{
		Logger.log( "Opening additional table: " + tablePath );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		TableColumns.addLabelImageIdColumn( columns, TableColumnNames.LABEL_IMAGE_ID, imageID );
		return columns;
	}

	public static Map< String, List< String > > createColumnsForMerging( List< TableRowImageSegment > segments, Map< String, List< String > > newColumns )
	{
		final ArrayList< String > segmentIdColumn = TableColumns.getColumn( segments, TableColumnNames.SEGMENT_LABEL_ID );
		final ArrayList< String > imageIdColumn = TableColumns.getColumn( segments, TableColumnNames.LABEL_IMAGE_ID );
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		referenceColumns.put( TableColumnNames.LABEL_IMAGE_ID, imageIdColumn );
		referenceColumns.put( TableColumnNames.SEGMENT_LABEL_ID, segmentIdColumn );

		// deal with the fact that the label ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( segmentIdColumn );
		MoBIEHelper.toDoubleStrings( newColumns.get( TableColumnNames.SEGMENT_LABEL_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, newColumns );

		return columnsForMerging;
	}
}
