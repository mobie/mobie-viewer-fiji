package de.embl.cba.mobie.display;

import de.embl.cba.mobie.grid.AnnotatedIntervalTableRow;
import de.embl.cba.mobie.source.StorageLocation;
import de.embl.cba.mobie.table.TableDataFormat;

import java.util.List;
import java.util.Map;

public class SourceAnnotationDisplay extends TableDisplay< AnnotatedIntervalTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources;
	protected List< String > selectedSourceAnnotationIds;
	protected Map< TableDataFormat, StorageLocation > tableData;

	// Runtime
	public List< String > getSelectedSourceAnnotationIds()
	{
		return selectedSourceAnnotationIds;
	}

	// Getters for the serialised fields
	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	public SourceAnnotationDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedSourceAnnotationIds = selectedSegmentIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param segmentationDisplay
	 */
	public SourceAnnotationDisplay( SourceAnnotationDisplay segmentationDisplay )
	{
		// TODO: We should use the TableDisplay as much as possible!
	}
}
