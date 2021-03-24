package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

public class SegmentationDisplay extends SourceDisplay
{
	public double alpha;
	public String lut;

	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient ColoringModelWrapper< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
}
