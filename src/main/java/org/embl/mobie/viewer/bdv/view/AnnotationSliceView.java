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
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotation.SliceViewAnnotationSelector;
import org.embl.mobie.viewer.color.AnnotationConverter;
import org.embl.mobie.viewer.color.ColoringListener;
import org.embl.mobie.viewer.color.VolatileAnnotationConverter;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.BoundarySource;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.source.VolatileBoundarySource;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import net.imglib2.Volatile;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;

public class AnnotationSliceView< A extends Annotation > extends AbstractSliceView implements ColoringListener, SelectionListener< A >
{
	protected final AnnotationDisplay< A > display;

	public AnnotationSliceView( MoBIE moBIE, AnnotationDisplay< A > display )
	{
		super( moBIE, display );
		this.display = display;
		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		for ( Image< AnnotationType< A > > image : display.getImages() )
		{
			// create volatile sac
			//
			final Source< ? extends Volatile< ? extends AnnotationType< ? > > > volatileSource = image.getSourcePair().getVolatileSource();
			final VolatileBoundarySource volatileBoundarySource = new VolatileBoundarySource( volatileSource );
			final VolatileAnnotationConverter volatileAnnotationConverter = new VolatileAnnotationConverter( display.coloringModel );
			SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileBoundarySource, volatileAnnotationConverter );

			// create non-volatile sac
			//
			final Source< ? extends  AnnotationType< ? > > source = image.getSourcePair().getSource();
			final BoundarySource boundarySource = new BoundarySource( source );
			final AnnotationConverter< ? > annotationConverter = new AnnotationConverter<>( display.coloringModel );

			// combine volatile and non-volatile sac
			final SourceAndConverter sourceAndConverter = new SourceAndConverter( boundarySource, annotationConverter, volatileSourceAndConverter );

			show( sourceAndConverter );

			if ( display instanceof SegmentationDisplay )
			{
				// below is needed for the bdv context menu
				// that configures the volume rendering
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, SegmentsVolumeViewer.class.getName(), ((SegmentationDisplay) display ).segmentsVolumeViewer );
			}
		}
	}

	private void show( SourceAndConverter< ? > sourceAndConverter )
	{
		configureAnnotationRendering( sourceAndConverter );
		display.sliceViewer.show( sourceAndConverter, display );
	}

	private void configureAnnotationRendering( SourceAndConverter< ? > sourceAndConverter )
	{
		final boolean showAsBoundaries = display.isShowAsBoundaries();
		final float boundaryThickness = display.getBoundaryThickness();
		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		boundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		if ( sourceAndConverter.asVolatile() != null )
		{
			final VolatileBoundarySource volatileBoundarySource = SourceHelper.unwrapSource( sourceAndConverter.asVolatile().getSpimSource(), VolatileBoundarySource.class );
			volatileBoundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		}
	}

	@Override
	public synchronized void selectionChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public void coloringChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	public synchronized void focusEvent( A selection, Object initiator )
	{
		if ( initiator instanceof SliceViewAnnotationSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		state.setCurrentTimepoint( selection.timePoint() );
		final double[] position = selection.anchor();

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				SliceViewLocationChanger.animationDurationMillis ).run();
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( getSliceViewer().getBdvHandle().getViewerPanel() );
	}
}
