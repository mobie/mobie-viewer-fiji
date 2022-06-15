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
package org.embl.mobie.viewer.display;

import de.embl.cba.tables.TableColumns;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.annotate.RegionsAdapter;
import org.embl.mobie.viewer.annotate.RegionCreator;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.bdv.view.RegionSliceView;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.TableRowsTableModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionDisplay extends AnnotationDisplay< RegionTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources; // annotationId to sources
	protected List< String > selectedRegionIds;
	protected Map< TableDataFormat, StorageLocation > tableData;

	// Runtime
	public transient RegionsAdapter tableRowsAdapter;
	public transient RegionSliceView sliceView;

	public List< String > getSelectedRegionIds()
	{
		return selectedRegionIds;
	}

	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	@Override
	public List< String > getSources()
	{
		final ArrayList< String > sources = new ArrayList<>();
		sources.add( getName() );
		return sources;
	}

	@Override
	public AnnotationSliceView< ? > getSliceView()
	{
		return sliceView;
	}

	// Needed for Gson
	public RegionDisplay() {}

	// Needed for Gson
	public RegionDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, boolean showAsBoundaries, float boundaryThickness  )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedRegionIds = selectedSegmentIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
		this.showAsBoundaries = showAsBoundaries;
		this.boundaryThickness = boundaryThickness;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param regionDisplay
	 */
	public RegionDisplay( RegionDisplay regionDisplay )
	{
		setAnnotationSettings( regionDisplay );

		this.sources = new HashMap<>();
		this.sources.putAll( regionDisplay.sources );

		Set< RegionTableRow > currentSelectedRows = regionDisplay.selectionModel.getSelected();
		if ( currentSelectedRows != null && currentSelectedRows.size() > 0 ) {
			ArrayList<String> selectedIds = new ArrayList<>();
			for ( RegionTableRow row : currentSelectedRows ) {
				selectedIds.add( row.timePoint() + ";" + row.name() );
			}
			this.selectedRegionIds = selectedIds;
		}

		this.tableData = new HashMap<>();
		this.tableData.putAll( regionDisplay.tableData );

		if ( regionDisplay.sliceView != null ) {
			this.visible = regionDisplay.sliceView.isVisible();
		}
	}

	// It is important that this is called after
	// all the sourceAndConverter are registered
	// in MoBIE
	public void initTableRows( )
	{
		// read
		final List< Map< String, List< String > > > tableColumns = new ArrayList<>();
		for ( String table : tables )
		{
			final Map< String, List< String > > columns = openTable( table );
			tableColumns.add( columns );
		}

		// primary table
		System.out.println("Creating regions for " + name + "..." );
		final RegionCreator regionCreator = new RegionCreator( tableColumns.get( 0 ), sources, ( String sourceName ) -> moBIE.sourceNameToSourceAndConverter().get( sourceName ) );
		final List< RegionTableRow > regionTableRows = regionCreator.getRegionTableRows();
		tableRows = new TableRowsTableModel( regionTableRows );

		// optional secondary table(s)
		for ( int i = 1; i < tableColumns.size(); ++i )
		{
			tableRows.mergeColumns( tableColumns.get( i ) );
		}

		tableRowsAdapter = new RegionsAdapter( tableRows.getTableRows() );
	}

	private Map< String, List< String > > openTable( String table )
	{
		String tablePath = IOHelper.combinePath( moBIE.getTableRoot(), moBIE.getDatasetName(), getTableDataFolder( TableDataFormat.TabDelimitedFile ), table );
		tablePath = MoBIEHelper.resolvePath( tablePath );

		final Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );

		// deal with the fact that the region ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		// MoBIEHelper.toDoubleStrings( columns.get( TableColumnNames.REGION_ID ) );
		// 2022-June-15 Commented out the above,
		// because the regionIds also occur in
		// protected Map< String, List< String > > sources; // annotationId to sources
		// where again there could be a different convention
		// regionIds should not be numbers anyway but something more
		// meaningful! (for labelIds that is a different story, see
		// SegmentationDisplay).

		return columns;
	}

	@Override
	public void mergeColumns( String tableFileName )
	{
		final Map< String, List< String > > columns = openTable( tableFileName );
		tableRows.mergeColumns( columns );
	}

	@Override
	public void mergeColumns( Map< String, List< String > > columns )
	{
		tableRows.mergeColumns( columns );
	}
}
