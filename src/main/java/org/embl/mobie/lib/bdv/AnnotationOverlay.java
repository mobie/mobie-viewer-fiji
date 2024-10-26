/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.bdv;

import bdv.util.*;
import bdv.viewer.ViewerState;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.select.Listeners;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.ArrayList;

public class AnnotationOverlay< A extends Annotation > extends BdvOverlay
{
	private final SliceViewer sliceViewer;
	private final ArrayList< A > annotations;
	private final String annotationColumn;
	private BdvOverlaySource< AnnotationOverlay > overlaySource;
	public static final int MAX_FONT_SIZE = 20;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, MAX_FONT_SIZE );

	protected final Listeners.SynchronizedList< ActiveListener > listeners
			= new Listeners.SynchronizedList< ActiveListener >(  );
	private AffineTransform3D viewerTransform;

	public AnnotationOverlay( SliceViewer sliceViewer, ArrayList< A > annotations, String annotationColumn )
	{
		this.sliceViewer = sliceViewer;
		this.annotations = annotations;
		this.annotationColumn = annotationColumn;

		this.overlaySource = BdvFunctions.showOverlay(
				this,
				"annotationOverlay",
				BdvOptions.options().addTo( sliceViewer.getBdvHandle() ) );

		// The below seems needed probably due a bug:
		// https://imagesc.zulipchat.com/#narrow/stream/327326-BigDataViewer/topic/BdvOverlay.20and.20Timepoints
		// https://github.com/mobie/mobie-viewer-fiji/issues/976
		sliceViewer.updateTimepointSlider();
	}

	public void close()
	{
		overlaySource.removeFromBdv();
		sliceViewer.getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	protected void draw( Graphics2D g )
	{
		final ViewerState viewerState = sliceViewer.getBdvHandle().getViewerPanel().state().snapshot();
		viewerTransform = viewerState.getViewerTransform();
		FinalRealInterval viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( sliceViewer.getBdvHandle() );
		double[] min = viewerInterval.minAsDoubleArray();
		double[] max = viewerInterval.maxAsDoubleArray();

		// add some extent along the z-axis (which is otherwise 0)
		double zMargin = ( max[ 0 ] - min[ 0 ] ) / 100; // FIXME: how much??
		min[ 2 ] -= zMargin;
		max[ 2 ] += zMargin;
		FinalRealInterval expandedViewerGlobalInterval = new FinalRealInterval( min, max );

		ArrayList< A > visibleAnnotations = new ArrayList<>();
		for ( A annotation : annotations )
		{
			if ( Intervals.contains( expandedViewerGlobalInterval, annotation ) )
			{
				visibleAnnotations.add( annotation );
			}
		}

		for ( A annotation : visibleAnnotations )
		{
			if ( annotation instanceof AnnotatedRegion )
			{
				// use the bounds
				final RealMaskRealInterval mask = ( ( AnnotatedRegion ) annotation ).getMask();
				FinalRealInterval bounds = viewerTransform.estimateBounds( mask );

				OverlayStringItem item = OverlayHelper.itemFromBounds(
						g,
						bounds,
						annotation.getValue( annotationColumn ).toString(),
						font
				);

				OverlayHelper.drawTextWithBackground( g, item );
			}
			else
			{
				// only use the location
				double[] canvasPosition = new double[ 3 ];
				viewerTransform.apply( annotation.positionAsDoubleArray(), canvasPosition );

				OverlayStringItem item = OverlayHelper.itemFromLocation(
						g,
						annotation.getValue( annotationColumn ).toString(),
						canvasPosition,
						visibleAnnotations.size(),
						font );

				OverlayHelper.drawTextWithBackground( g, item );
			}
		}
	}


	public void addListener( ActiveListener activeListener )
	{
		listeners.add( activeListener );
	}
}
