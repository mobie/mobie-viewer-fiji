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
package org.embl.mobie.viewer.serialize.display;

import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.color.lut.LUTs;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SegmentationDisplay< AS extends AnnotatedSegment > extends AbstractAnnotationDisplay< AS >
{
	// Serialization
	protected List< String > sources; // label mask images
	protected Set< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;
	protected Double[] resolution3dView;

	// Runtime
	// TODO: below is almost not needed
	public transient SegmentsVolumeViewer< AS > segmentsVolumeViewer;

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	public Double[] getResolution3dView(){ return resolution3dView; }

	@Override
	public Set< String > selectedAnnotationIds()
	{
		return selectedSegmentIds;
	}

	@Override
	public void setSelectedAnnotationIds( Set< String > selectedAnnotationIds )
	{
		this.selectedSegmentIds = selectedAnnotationIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}

	// Gson deserialization
	public SegmentationDisplay()
	{
		super();
	}

	public SegmentationDisplay( String name, List< String > sources )
	{
		super();
		this.name = name;
		this.sources = sources;
	}

	public SegmentationDisplay(
			String name,
			@Nullable Double opacity,
			List< String > sources,
			@Nullable String lut,
			@Nullable String colorByColumn,
			@Nullable Double[] valueLimits,
			@Nullable Set< String > selectedSegmentIds,
			boolean showSelectedSegmentsIn3d,
			boolean showScatterPlot,
			@Nullable String[] scatterPlotAxes,
			@Nullable List< String > tables,
			@Nullable Double[] resolution3dView )
	{
		this.name = name;
		this.opacity = opacity == null ? 0.5 : opacity;
		this.sources = sources;
		this.lut = lut == null ? LUTs.GLASBEY : lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedSegmentIds = selectedSegmentIds;
		this.showSelectedSegmentsIn3d = showSelectedSegmentsIn3d;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.additionalTables = tables;
		this.resolution3dView = resolution3dView;
	}

	// Create a serializable copy
	public SegmentationDisplay( SegmentationDisplay< ? extends AnnotatedSegment > segmentationDisplay )
	{
		// set properties common to all AnnotationDisplays
		super( segmentationDisplay );

		// set properties specific to SegmentationDisplay
		//
		this.sources = new ArrayList<>();
		this.sources.addAll( segmentationDisplay.sources );

		if ( segmentationDisplay.segmentsVolumeViewer != null )
		{
			this.showSelectedSegmentsIn3d = segmentationDisplay.segmentsVolumeViewer.isShowSegments();

			double[] voxelSpacing = segmentationDisplay.segmentsVolumeViewer.getVoxelSpacing();
			if ( voxelSpacing != null ) {
				resolution3dView = new Double[voxelSpacing.length];
				for (int i = 0; i < voxelSpacing.length; i++) {
					resolution3dView[i] = voxelSpacing[i];
				}
			}
		}
	}
}
