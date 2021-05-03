package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.color.opacity.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.plot.ScatterPlotViewer;
import de.embl.cba.mobie2.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie2.volume.SegmentsVolumeView;
import de.embl.cba.mobie2.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SegmentationDisplay extends Display
{
	// Serialization
	private String lut = ColoringLuts.GLASBEY;
	private String colorByColumn;
	private Double[] valueLimits = new Double[]{ null, null };
	private List< String > selectedSegmentIds;
	private boolean showSelectedSegmentsIn3d = false;
	private boolean showScatterPlot = false;
	private String[] scatterPlotAxes = new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y };
	private List< String > tables;

	// Runtime
	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient MoBIEColoringModel< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
	public transient ScatterPlotViewer< TableRowImageSegment > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;
	public transient SegmentsVolumeView< TableRowImageSegment > segmentsVolumeViewer;
	public transient SegmentationImageSliceView< TableRowImageSegment > segmentationImageSliceView;

	public String getLut()
	{
		return lut;
	}

	public String getColorByColumn()
	{
		return colorByColumn;
	}

	public Double[] getValueLimits()
	{
		return valueLimits;
	}

	public List< String > getSelectedSegmentIds()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}

	public boolean showScatterPlot()
	{
		return showScatterPlot;
	}

	public String[] getScatterPlotAxes()
	{
		return scatterPlotAxes;
	}

	public List< String > getTables()
	{
		return tables;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param segmentationDisplay
	 */
	public SegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		this.name = segmentationDisplay.name;
		this.sources = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : segmentationDisplay.sourceAndConverters )
		{
			sources.add( sourceAndConverter.getSpimSource().getName() );
		}

		final SourceAndConverter< ? > sourceAndConverter = segmentationDisplay.sourceAndConverters.get( 0 );

		if( sourceAndConverter.getConverter() instanceof AdjustableOpacityColorConverter)
		{
			this.opacity = ( ( AdjustableOpacityColorConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		this.lut = segmentationDisplay.tableViewer.getColoringLUTName();
		this.colorByColumn = segmentationDisplay.tableViewer.getColoringColumnName();

		Double[] valueLimits = new Double[2];
		double[] currentValueLimits = segmentationDisplay.tableViewer.getColorByColumnValueLimits();
		for (int i=0; i< currentValueLimits.length; i++) {
			valueLimits[i] = currentValueLimits[i];
		}
		this.valueLimits = valueLimits;

		Set<TableRowImageSegment> currentSelectedSegments = segmentationDisplay.selectionModel.getSelected();
		if (currentSelectedSegments != null) {
			ArrayList<String> selectedSegmentIds = new ArrayList<>();
			for (TableRowImageSegment segment : currentSelectedSegments) {
				selectedSegmentIds.add( String.valueOf( segment.labelId() ) );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}

		this.showSelectedSegmentsIn3d = segmentationDisplay.segmentsVolumeViewer.getShowSegments();
		this.showScatterPlot = segmentationDisplay.scatterPlotViewer.getShowScatterPlot();
		this.scatterPlotAxes = segmentationDisplay.scatterPlotViewer.getSelectedColumns();
		this.tables = segmentationDisplay.tableViewer.getAdditionalTables();
	}

}
