package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.view.ScatterPlotViewer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;

public class SegmentationDisplay extends SourceDisplay
{
	private final String lut; // property of coloringModelWrapper
	private final List< String > selectedSegmentIds;

	// The actual classes needed at runtime
	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient MoBIEColoringModel< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
	public transient ScatterPlotViewer< TableRowImageSegment > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;

	// For serialization
	public SegmentationDisplay( String name, List< String > sources, double alpha, String lut, List< String > selectedSegmentIds )
	{
		super( isExclusive, name, alpha, sources );
		this.lut = lut;
		this.selectedSegmentIds = selectedSegmentIds;
	}

	public String getLut()
	{
		return lut;
	}

	public List< String > getSelectedSegmentIds()
	{
		return selectedSegmentIds;
	}
}
