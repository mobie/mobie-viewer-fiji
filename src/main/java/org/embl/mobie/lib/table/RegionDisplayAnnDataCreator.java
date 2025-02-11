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
package org.embl.mobie.lib.table;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.RegionTableSource;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedImages;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedRegionCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionDisplayAnnDataCreator
{
	private final MoBIE moBIE;
	private final RegionDisplay< ? > regionDisplay;

	private Table table;
	private StorageLocation tableLocation;
	private TableDataFormat tableFormat;

	public RegionDisplayAnnDataCreator( MoBIE moBIE, RegionDisplay< ? > regionDisplay )
	{
		this.moBIE = moBIE; // TODO it would feel cleaner not to have to pass moBIE here
		this.regionDisplay = regionDisplay;
	}

	public AnnData< AnnotatedRegion > createAnnData()
	{
		if (  regionDisplay.tableSource == null )
			createTable();
		else
			fetchTable();

		final TableSawAnnotationCreator< TableSawAnnotatedImages > annotationCreator = new TableSawAnnotatedRegionCreator( table, regionDisplay.sources, regionDisplay.getRelativeDilation() );

		final TableSawAnnotationTableModel< AnnotatedRegion > tableModel = new TableSawAnnotationTableModel( regionDisplay.getName(), annotationCreator, tableLocation, tableFormat, table );

		final DefaultAnnData< AnnotatedRegion > annData = new DefaultAnnData<>( tableModel );

		return annData;
	}

	private void fetchTable()
	{
		final RegionTableSource regionTableSource = ( RegionTableSource ) DataStore.getRawData( regionDisplay.tableSource );
		tableLocation = moBIE.getTableLocation( regionTableSource.tableData );
		tableFormat = moBIE.getTableDataFormat( regionTableSource.tableData );
		table = TableOpener.open( tableLocation, tableFormat );

		// only keep the subset of rows (regions)
		// that are actually referred to in regionDisplay
		final Set< String > regionIDs = regionDisplay.sources.keySet();
		final ArrayList< Integer > dropRows = new ArrayList<>();
		final int rowCount = table.rowCount();
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
		{
			final String regionId = table.row( rowIndex ).getObject( ColumnNames.REGION_ID ).toString();
			if ( ! regionIDs.contains( regionId ) )
				dropRows.add( rowIndex );
		}

		if ( dropRows.size() > 0 )
			table = table.dropRows( dropRows.stream().mapToInt( i -> i ).toArray() );
	}

	private void createTable()
	{
		tableLocation = new StorageLocation();
		tableFormat = TableDataFormat.Table;
		table = Table.create( regionDisplay.getName() );

		// copy the regions into a list to
		// fix the order
		final List< String > regions = new ArrayList<>( regionDisplay.sources.keySet() );

		table.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );

		if ( regionDisplay.getSourceNamesRegex() != null )
		{
			addSourceNameParsingColumns( regions );
		}
	}

	private void addSourceNameParsingColumns( List< String > regionNames )
	{
		final Pattern pattern = Pattern.compile( regionDisplay.getSourceNamesRegex() );
		final HashMap< String, List< String > > columnToValues = new HashMap<>();
		final List< String > groupNames = MoBIEHelper.getNamedGroups( regionDisplay.getSourceNamesRegex() );

		Integer groupCount = null;
		Boolean useNames = null;

		for ( String region : regionNames )
		{
			final String source = regionDisplay.sources.get( region ).get( 0 );
			final Matcher matcher = pattern.matcher( source );
			if ( ! matcher.matches() )
			{
				throw new RuntimeException( "Could not match regex for " + source );
			}

			// use the first region to initialise
			// whether there are named groups
			// to be used for the column names
			if ( groupCount == null ) groupCount = matcher.groupCount();
			if ( useNames == null ) useNames = groupNames.size() == groupCount;

			for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
			{
				final String value = matcher.group( groupIndex + 1 );

				String column = useNames ? groupNames.get( groupIndex ) : "" + groupIndex;

				if ( ! columnToValues.containsKey( column ) )
					columnToValues.put( column, new ArrayList<>() );

				columnToValues.get( column ).add( value );
			}
		}

		for ( String column : columnToValues.keySet() )
		{
			table.addColumns( StringColumn.create( column, columnToValues.get( column ) ) );
		}
	}

}

