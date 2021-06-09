package de.embl.cba.mobie.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie.color.MoBIEColoringModel;
import de.embl.cba.mobie.color.opacity.AdjustableOpacityColorConverter;
import de.embl.cba.mobie.segment.SegmentAdapter;
import de.embl.cba.mobie.plot.ScatterPlotViewer;
import de.embl.cba.mobie.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie.volume.SegmentsVolumeView;
import de.embl.cba.mobie.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SegmentationSourceDisplay extends SourceDisplay
{
	// Serialization
	private String lut = ColoringLuts.GLASBEY;
	private String colorByColumn;
	private Double[] valueLimits = new Double[]{ null, null };
	private List< String > selectedSegmentIds;
	private boolean showSelectedSegmentsIn3d = false;
	private boolean showScatterPlot = false;
	private String[] scatterPlotAxes = new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y };
	private List< String > tables; // table columns in addition to default

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

	public SegmentationSourceDisplay( String name, double opacity, List< String > sources,
									  String lut, String colorByColumn, Double[] valueLimits,
									  List< String > selectedSegmentIds, boolean showSelectedSegmentsIn3d,
									  boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedSegmentIds = selectedSegmentIds;
		this.showSelectedSegmentsIn3d = showSelectedSegmentsIn3d;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param segmentationDisplay
	 */
	public SegmentationSourceDisplay( SegmentationSourceDisplay segmentationDisplay )
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

		this.lut = segmentationDisplay.coloringModel.getARGBLutName();

		final ColoringModel< TableRowImageSegment > wrappedColoringModel = segmentationDisplay.coloringModel.getWrappedColoringModel();

		if ( wrappedColoringModel instanceof ColumnColoringModel )
		{
			this.colorByColumn = (( ColumnColoringModel ) wrappedColoringModel).getColumnName();
		}

		if ( wrappedColoringModel instanceof NumericColoringModel )
		{
			this.valueLimits = new Double[2];
			NumericColoringModel numericColoringModel = ( NumericColoringModel ) ( wrappedColoringModel );
			valueLimits[0] = numericColoringModel.getMin();
			valueLimits[1] = numericColoringModel.getMax();
		}

		Set<TableRowImageSegment> currentSelectedSegments = segmentationDisplay.selectionModel.getSelected();
		if (currentSelectedSegments != null) {
			ArrayList<String> selectedSegmentIds = new ArrayList<>();
			for (TableRowImageSegment segment : currentSelectedSegments) {
				selectedSegmentIds.add( String.valueOf( segment.labelId() ) );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}

		this.showSelectedSegmentsIn3d = segmentationDisplay.segmentsVolumeViewer.getShowSegments();
		this.showScatterPlot = segmentationDisplay.scatterPlotViewer.isVisible();
		this.scatterPlotAxes = segmentationDisplay.scatterPlotViewer.getSelectedColumns();
		this.tables = segmentationDisplay.tableViewer.getAdditionalTables();
	}

}
