package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.view.ScatterPlotViewer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;

public class SegmentationDisplay extends SourceDisplay
{
	public double alpha;
	public String lut;

	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient ColoringModelWrapper< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
	public transient ScatterPlotViewer< TableRowImageSegment > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;

	public SegmentationDisplay()
	{
		super( name, sources );
	}
}
