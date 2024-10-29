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
package org.embl.mobie.lib.bdv.overlay;

import bdv.util.*;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.ArrayList;

public class AnnotatedRegionsOverlay< AR extends AnnotatedRegion >
		extends BdvOverlay implements AnnotationOverlay
{
	private final SliceViewer sliceViewer;
	private final ArrayList< AR > annotatedRegions;
	private final String annotationColumn;
	private BdvOverlaySource< AnnotatedRegionsOverlay > overlaySource;
	public static final int MAX_FONT_SIZE = 20;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, MAX_FONT_SIZE );

	public AnnotatedRegionsOverlay(
			SliceViewer sliceViewer,
			ArrayList< AR > annotatedRegions,
			String annotationColumn )
	{
		this.sliceViewer = sliceViewer;
		this.annotatedRegions = annotatedRegions;
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

	@Override
	public void close()
	{
		overlaySource.removeFromBdv();
		sliceViewer.getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	protected void draw( Graphics2D g )
	{
		BdvHandle bdvHandle = sliceViewer.getBdvHandle();
		final AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();

		FinalRealInterval viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		ArrayList< AR > visibleRegions = new ArrayList<>();
		for ( AR annotatedRegion : annotatedRegions )
		{
			if ( Intervals.isEmpty( Intervals.intersect( viewerInterval, annotatedRegion.getMask() ) ) )
			{
				continue; // The region is not currently visible on the canvas
			}
			else
			{
				visibleRegions.add( annotatedRegion );
			}
		}

		for ( AR annotation : visibleRegions )
		{
			final RealMaskRealInterval mask = annotation.getMask();
			FinalRealInterval bounds = viewerTransform.estimateBounds( mask );

			OverlayTextItem item = OverlayHelper.itemFromBounds(
					g,
					bounds,
					annotation.getValue( annotationColumn ).toString(),
					font
			);

			OverlayHelper.drawTextWithBackground( g, item );
		}
	}
}
