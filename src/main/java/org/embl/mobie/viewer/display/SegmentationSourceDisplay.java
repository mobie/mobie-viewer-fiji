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
	public transient SegmentationSliceView sliceView;

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

	// Needed for Gson
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
		fetchCurrentSettings( segmentationDisplay );

		this.sources = new ArrayList<>();
		this.sources.addAll( segmentationDisplay.sourceNameToSourceAndConverter.keySet() );

		final SourceAndConverter< ? > sourceAndConverter =
				segmentationDisplay.sourceNameToSourceAndConverter.values().iterator().next();

		this.blendingMode = (BlendingMode) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );

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

		Set<TableRowImageSegment> currentSelectedSegments = segmentationDisplay.selectionModel.getSelected();
		if (currentSelectedSegments != null) {
			ArrayList<String> selectedSegmentIds = new ArrayList<>();
			for (TableRowImageSegment segment : currentSelectedSegments) {
				selectedSegmentIds.add( segment.imageId() + ";" + segment.timePoint() + ";" + segment.labelId() );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}
	}
}
