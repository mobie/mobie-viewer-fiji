package de.embl.cba.mobie.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.color.LabelConverter;
import de.embl.cba.mobie.grid.AnnotatedIntervalTableRow;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SourceAnnotationDisplay extends TableDisplay< AnnotatedIntervalTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources;
	protected List< String > selectedSourceAnnotationIds;

	// Runtime

	public List< String > getSelectedSourceAnnotationIds()
	{
		return selectedSourceAnnotationIds;
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
