package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.view.ScatterPlotViewer;
import de.embl.cba.mobie2.view.SegmentationSliceView;
import de.embl.cba.mobie2.view.Segments3DView;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;

public class SegmentationDisplay extends Display
{
	// Serialization
	private String lut;
	private List< String > selectedSegmentIds;
	private boolean showSelectedSegmentsIn3d;

	// Runtime
	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient MoBIEColoringModel< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
	public transient ScatterPlotViewer< TableRowImageSegment > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;
	public transient Segments3DView< TableRowImageSegment > segmentsVolumeViewer;
	public transient SegmentationSliceView< TableRowImageSegment > segmentationSliceView;

	public String getLut()
	{
		return lut;
	}

	public List< String > getSelectedSegmentIds()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}
}
