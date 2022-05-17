package org.embl.mobie.viewer.display;

import org.embl.mobie.viewer.annotate.AnnotatedMaskAdapter;
import org.embl.mobie.viewer.annotate.AnnotatedMaskTableRow;
import org.embl.mobie.viewer.bdv.view.RegionSliceView;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionDisplay extends AnnotationDisplay< AnnotatedMaskTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources;
	protected List< String > selectedAnnotationIds;
	protected Map< TableDataFormat, StorageLocation > tableData;

	// Runtime
	public transient AnnotatedMaskAdapter annotatedMaskAdapter;
	public transient RegionSliceView sliceView;

	public List< String > getSelectedAnnotationIds()
	{
		return selectedAnnotationIds;
	}

	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	public Map< String, List< String > > getAnnotationIdToSources()
	{
		return sources;
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

	// TODO: Looks like we do not need it? Maybe for the interactive grid view?
	public RegionDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedAnnotationIds = selectedSegmentIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param regionDisplay
	 */
	public RegionDisplay( RegionDisplay regionDisplay )
	{
		fetchCurrentSettings( regionDisplay );

		this.sources = new HashMap<>();
		this.sources.putAll( regionDisplay.sources );

		Set< AnnotatedMaskTableRow > currentSelectedRows = regionDisplay.selectionModel.getSelected();
		if ( currentSelectedRows != null && currentSelectedRows.size() > 0 ) {
			ArrayList<String> selectedIds = new ArrayList<>();
			for ( AnnotatedMaskTableRow row : currentSelectedRows ) {
				selectedIds.add( row.timePoint() + ";" + row.name() );
			}
			this.selectedAnnotationIds = selectedIds;
		}

		this.tableData = new HashMap<>();
		this.tableData.putAll( regionDisplay.tableData );

		if ( regionDisplay.sliceView != null ) {
			this.visible = regionDisplay.sliceView.isVisible();
		}
	}

}
