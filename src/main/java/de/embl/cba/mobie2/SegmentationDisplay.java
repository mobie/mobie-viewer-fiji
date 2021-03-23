package de.embl.cba.mobie2;

import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

public class SegmentationDisplay extends SourceDisplay
{
	public double alpha;
	public String color;

	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient MoBIEColoringModel< TableRowImageSegment > coloringModel;
}
