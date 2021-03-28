package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.view.ScatterPlotViewer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;

public class SegmentationDisplay extends SourceDisplay
{
	private final double alpha;
	private final String lut;
	private final List< String > selectedSegmentIds;

	// TODO: rework according to ImageDisplay
	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient ColoringModelWrapper< TableRowImageSegment > coloringModel;
	public transient TableViewer< ? extends TableRow > tableViewer;
	public transient ScatterPlotViewer< ? extends TableRow > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;
	public transient SegmentAdapter< ? extends ImageSegment > segmentAdapter;

	// For serialization
	public SegmentationDisplay( String name, List< String > sources, double alpha, String lut, List< String > selectedSegmentIds )
	{
		super( name, sources );
		this.alpha = alpha;
		this.lut = lut;
		this.selectedSegmentIds = selectedSegmentIds;
	}

	public double getAlpha()
	{
		return alpha;
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
