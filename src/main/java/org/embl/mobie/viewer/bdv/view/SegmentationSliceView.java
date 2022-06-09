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
package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.LazySourceAndConverter;
import org.embl.mobie.viewer.transform.MergedGridSource;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;

public class SegmentationSliceView extends AnnotationSliceView< TableRowImageSegment >
{
	public SegmentationSliceView( MoBIE moBIE, SegmentationDisplay display )
	{
		super( moBIE, display );

		for ( String name : display.getSources() )
		{
			final SourceAndConverter< ? > sourceAndConverter = moBIE.sourceNameToSourceAndConverter().get( name );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( display, sourceAndConverter );

			show( labelSourceAndConverter );
		}
	}

	private SourceAndConverter< ? > asLabelSourceAndConverter( SegmentationDisplay display, SourceAndConverter< ? > sourceAndConverter )
	{
		LabelConverter labelConverter = getLabelConverter( display, sourceAndConverter );

		SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

		return labelSourceAndConverter;
	}

	private LabelConverter getLabelConverter( SegmentationDisplay display, SourceAndConverter< ? > sourceAndConverter )
	{
		if ( MergedGridSource.instanceOf( sourceAndConverter ) )
		{
			// The source name is not the one from which the
			// image segments should be fetched.
			// Thus, the constructor where the source name
			// is determined from encoding in the label is chosen.
			return new LabelConverter(
					display.segmentAdapter,
					display.selectionColoringModel );
		}
		else
		{
			return new LabelConverter(
					display.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					display.selectionColoringModel );
		}
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	@Override
	public synchronized void focusEvent( TableRowImageSegment selection, Object initiator )
	{
		if ( initiator instanceof SliceViewRegionSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		state.setCurrentTimepoint( selection.timePoint() );

		final double[] position = new double[ 3 ];
		selection.localize( position );

		adaptPosition( position, selection.imageId() );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				SliceViewLocationChanger.animationDurationMillis ).run();
	}

	private void adaptPosition( double[] position, String sourceName )
	{
		final SourceAndConverter< ? > sourceAndConverter = moBIE.sourceNameToSourceAndConverter().get( sourceName );

		// get source transform
		AffineTransform3D sourceTransform = new AffineTransform3D();
		if ( sourceAndConverter instanceof LazySourceAndConverter )
			( ( LazySourceAndConverter ) sourceAndConverter ).setSourceTransform( sourceTransform );
		else
			sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );

		// get voxel dimensions
		final VoxelDimensions voxelDimensions;
		if ( sourceAndConverter instanceof LazySourceAndConverter )
			voxelDimensions = ( ( LazySourceAndConverter ) sourceAndConverter ).getVoxelDimensions();
		else
		    voxelDimensions = sourceAndConverter.getSpimSource().getVoxelDimensions();

		// remove scaling, because the positions are in scaled units
		final AffineTransform3D scalingTransform = new AffineTransform3D();
		scalingTransform.scale( voxelDimensions.dimension( 0 ), voxelDimensions.dimension( 1 ), voxelDimensions.dimension( 2 )  );
		sourceTransform.concatenate( scalingTransform.inverse() );

		// adapt position
		sourceTransform.apply( position, position );
	}
}
