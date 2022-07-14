/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mobie3.viewer.display;

import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mobie3.viewer.source.AnnotatedLabelMask;
import mobie3.viewer.bdv.view.AnnotationSliceView;
import mobie3.viewer.bdv.view.AnnotatedLabelMaskSliceView;
import mobie3.viewer.segment.LabelToSegmentMapper;
import mobie3.viewer.table.AnnotatedSegment;
import mobie3.viewer.volume.SegmentsVolumeViewer;
import net.imglib2.type.numeric.IntegerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AnnotatedImageSegmentsDisplay< T extends IntegerType< T >, AS extends AnnotatedSegment > extends AnnotationDisplay< AS >
{
	// Serialization
	protected List< String > sources; // label masks
	protected List< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;
	protected Double[] resolution3dView;

	// Runtime
	// TODO: below is almost not needed
	public transient LabelToSegmentMapper< AS > segmentMapper;
	public transient SegmentsVolumeViewer< AS > segmentsVolumeViewer;
	public transient AnnotatedLabelMaskSliceView sliceView;

	@Override
	public AnnotationSliceView< ? > getSliceView()
	{
		return sliceView;
	}

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	public Double[] getResolution3dView(){ return resolution3dView; }

	public List< String > getSelectedSegmentIds()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}

	// Needed for Gson
	public AnnotatedImageSegmentsDisplay(){}

	public AnnotatedImageSegmentsDisplay( String name, double opacity, List< String > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showSelectedSegmentsIn3d, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, Double[] resolution3dView )
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
	}

	/**
	 * Create a serializable copy
	 *
	 * @param annotatedImageSegmentsDisplay
	 */
	public AnnotatedImageSegmentsDisplay( AnnotatedImageSegmentsDisplay annotatedImageSegmentsDisplay )
	{
		set( annotatedImageSegmentsDisplay );

		this.sources = new ArrayList<>();
		this.sources.addAll( annotatedImageSegmentsDisplay.nameToSourceAndConverter.keySet() );

		if ( annotatedImageSegmentsDisplay.segmentsVolumeViewer != null )
		{
			this.showSelectedSegmentsIn3d = annotatedImageSegmentsDisplay.segmentsVolumeViewer.isShowSegments();

			double[] voxelSpacing = annotatedImageSegmentsDisplay.segmentsVolumeViewer.getVoxelSpacing();
			if ( voxelSpacing != null ) {
				resolution3dView = new Double[voxelSpacing.length];
				for (int i = 0; i < voxelSpacing.length; i++) {
					resolution3dView[i] = voxelSpacing[i];
				}
			}
		}

		Set<TableRowImageSegment> currentSelectedSegments = annotatedImageSegmentsDisplay.selectionModel.getSelected();
		if (currentSelectedSegments != null) {
			ArrayList<String> selectedSegmentIds = new ArrayList<>();
			for (TableRowImageSegment segment : currentSelectedSegments) {
				selectedSegmentIds.add( segment.imageId() + ";" + segment.timePoint() + ";" + (int) segment.labelId() );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}

		if ( annotatedImageSegmentsDisplay.sliceView != null ) {
			visible = annotatedImageSegmentsDisplay.sliceView.isVisible();
		}
	}

	// It is important that this is called
	// after all the images are registered
	// in MoBIE
	public void initTableModel( )
	{
		final List< String > columnChunks = getTables();
		if ( columnChunks.size() == 0 ) return;

		// TODO
		if ( sources.size() > 1 )
		{
			throw new UnsupportedOperationException("Table display for multiple sources not yet implemented!");
		}

		for ( String imageName : sources )
		{
			final AnnotatedLabelMask< T, AS > annotatedLabelMask = ( AnnotatedLabelMask ) moBIE.getImage( imageName );
			tableModel = annotatedLabelMask.getAnnData().getTable();

			for ( String columnChunk : columnChunks )
			{
				tableModel.loadColumns( columnChunk );
			}
		}
	}

}
