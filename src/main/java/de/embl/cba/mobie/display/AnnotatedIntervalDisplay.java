package de.embl.cba.mobie.display;

import de.embl.cba.mobie.annotate.AnnotatedIntervalAdapter;
import de.embl.cba.mobie.annotate.AnnotatedIntervalTableRow;
import de.embl.cba.mobie.bdv.view.AnnotatedIntervalSliceView;
import de.embl.cba.mobie.source.StorageLocation;
import de.embl.cba.mobie.table.TableDataFormat;

import java.util.List;
import java.util.Map;

public class AnnotatedIntervalDisplay extends AnnotatedRegionDisplay< AnnotatedIntervalTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources;
	protected List< String > selectedAnnotationIds;
	protected Map< TableDataFormat, StorageLocation > tableData;

	// Runtime
	public transient AnnotatedIntervalAdapter< AnnotatedIntervalTableRow > annotatedIntervalAdapter;
	public transient AnnotatedIntervalSliceView< AnnotatedIntervalTableRow > sliceView;

	// Getters for the serialised fields
	public List< String > getSelectedAnnotationIds()
	{
		return selectedAnnotationIds;
	}

	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	public Map< String, List< String > > getSources()
	{
		return sources;
	}

	public AnnotatedIntervalDisplay() {}

	public AnnotatedIntervalDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables )
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
	 * @param segmentationDisplay
	 */
	public AnnotatedIntervalDisplay( AnnotatedIntervalDisplay segmentationDisplay )
	{
		// TODO: We should use the TableDisplay as much as possible!
	}
}
