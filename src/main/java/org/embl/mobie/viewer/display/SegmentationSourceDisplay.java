package org.embl.mobie.viewer.display;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.segment.SegmentAdapter;
import org.embl.mobie.viewer.bdv.view.SegmentationSliceView;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.*;

public class SegmentationSourceDisplay extends AnnotatedRegionDisplay< TableRowImageSegment >
{
	// Serialization
	protected List< String > sources;
	protected List< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;
	protected Double[] resolution3dView;
	protected BlendingMode blendingMode;


	// Runtime
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;
	public transient SegmentsVolumeViewer< TableRowImageSegment > segmentsVolumeViewer;
	public transient SegmentationSliceView< TableRowImageSegment > sliceView;

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	public Double[] getResolution3dView(){ return resolution3dView; }

	public BlendingMode getBlendingMode()
	{
		return blendingMode;
	}

	public List< String > getSelectedTableRows()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}

	public SegmentationSourceDisplay(){}

	public SegmentationSourceDisplay( String name, double opacity, List< String > sources,
									  String lut, String colorByColumn, Double[] valueLimits,
									  List< String > selectedSegmentIds, boolean showSelectedSegmentsIn3d,
									  boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables,
									  Double[] resolution3dView, BlendingMode blendingMode )
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
		this.resolution3dView = resolution3dView;
		this.blendingMode = blendingMode;
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

		this.blendingMode = (BlendingMode) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );

		if( sourceAndConverter.getConverter() instanceof LabelConverter)
		{
			this.opacity = ( ( LabelConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		if ( segmentationDisplay.segmentsVolumeViewer != null )
		{
			this.showSelectedSegmentsIn3d = segmentationDisplay.segmentsVolumeViewer.getShowSegments();

			double[] voxelSpacing = segmentationDisplay.segmentsVolumeViewer.getVoxelSpacing();
			if ( voxelSpacing != null ) {
				resolution3dView = new Double[voxelSpacing.length];
				for (int i = 0; i < voxelSpacing.length; i++) {
					resolution3dView[i] = voxelSpacing[i];
				}
			}
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
				selectedSegmentIds.add( segment.imageId() + ";" + segment.timePoint() + ";" + segment.labelId() );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}

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
