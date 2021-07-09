package de.embl.cba.mobie.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie.color.LabelConverter;
import de.embl.cba.mobie.color.MoBIEColoringModel;
import de.embl.cba.mobie.segment.SegmentAdapter;
import de.embl.cba.mobie.plot.ScatterPlotViewer;
import de.embl.cba.mobie.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie.volume.SegmentsVolumeViewer;
import de.embl.cba.mobie.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SegmentationSourceDisplay extends TableDisplay< TableRowImageSegment >
{
	// Serialization
	protected List< String > sources;
	protected List< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;

	// Runtime
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;
	public transient SegmentsVolumeViewer< TableRowImageSegment > segmentsVolumeViewer;
	public transient SegmentationImageSliceView< TableRowImageSegment > segmentationImageSliceView;

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	public List< String > getSelectedTableRows()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
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
		// TODO: put as much as possible of this into TableDisplay, I guess it could be a protected method that takes < ? extends TableDisplay > as an input and then sets the fields.
		this.name = segmentationDisplay.name;
		this.sources = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : segmentationDisplay.sourceAndConverters )
		{
			sources.add( sourceAndConverter.getSpimSource().getName() );
		}

		final SourceAndConverter< ? > sourceAndConverter = segmentationDisplay.sourceAndConverters.get( 0 );

		if( sourceAndConverter.getConverter() instanceof LabelConverter )
		{
			this.opacity = ( ( LabelConverter ) sourceAndConverter.getConverter() ).getOpacity();
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
		this.tables = segmentationDisplay.tables;
		List<String> additionalTables = segmentationDisplay.tableViewer.getAdditionalTables();
		if ( additionalTables.size() > 0 ){
			if ( this.tables == null ) {
				this.tables = new ArrayList<>();
			}
			this.tables.addAll( additionalTables );
		}
	}

}
