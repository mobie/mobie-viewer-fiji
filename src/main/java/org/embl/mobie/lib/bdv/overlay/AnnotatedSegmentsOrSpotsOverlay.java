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
import bdv.viewer.ViewerState;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.ActiveListener;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.select.Listeners;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.ArrayList;

public class AnnotatedSegmentsOrSpotsOverlay< A extends Annotation >
		extends BdvOverlay implements AnnotationOverlay
{
	private final SliceViewer sliceViewer;
	private final ArrayList< A > annotations;
	private final String annotationColumn;
	private BdvOverlaySource< AnnotatedSegmentsOrSpotsOverlay > overlaySource;
	public static final int MAX_FONT_SIZE = 20;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, MAX_FONT_SIZE );

	public AnnotatedSegmentsOrSpotsOverlay(
			SliceViewer sliceViewer,
			ArrayList< A > annotations,
			String annotationColumn )
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

	@Override
	public void close()
	{
		overlaySource.removeFromBdv();
		sliceViewer.getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	protected void draw( Graphics2D g )
	{
		// TODO: If there are 10k annotations that slows down the BDV rendering

		BdvHandle bdvHandle = sliceViewer.getBdvHandle();
		final AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
		//double scale = Affine3DHelpers.extractScale( viewerTransform, 2 );
		int width = bdvHandle.getViewerPanel().getWidth();
		int height = bdvHandle.getViewerPanel().getHeight();

		if ( annotations == null || annotations.size() == 0 )
			return;

		OverlayTextItem item = new OverlayTextItem();
		double[] globalPosition = new double[ 3 ];
		double[] viewPosition = new double[ 3 ];

		for ( A annotation : annotations )
		{
			annotation.localize( globalPosition );
			viewerTransform.apply( globalPosition, viewPosition );

			if ( viewPosition[ 0 ] < 0
					|| viewPosition[ 1 ] < 0
				|| viewPosition[ 0 ] > width
					|| viewPosition[ 1 ] > height )
				continue;

			// final double depth = Math.abs( viewPosition[ 2 ] ) / scale;
			//System.out.println( text + ": " + depth + "; " + Math.abs( viewPosition[ 2 ] ));
			//float computedFontSize = ( float ) ( 3.0 * AnnotationOverlay.MAX_FONT_SIZE / Math.sqrt( numAnnotations ) );

			float fontSize = (float) ( MAX_FONT_SIZE - Math.abs( viewPosition[ 2 ] ) );
			if ( fontSize < 2 )
				continue;

			g.setFont( font.deriveFont( Math.min( MAX_FONT_SIZE, fontSize ) ) );
			item.text = annotation.getValue( annotationColumn ).toString();
			item.width = g.getFontMetrics().stringWidth( item.text );
			item.height = g.getFontMetrics().getHeight();
			item.x = ( int ) ( viewPosition[ 0 ]  - item.width / 2 );
			item.y = ( int ) ( viewPosition[ 1 ] + 1.5 * item.height ); // paint a bit below (good for points)

			OverlayHelper.drawTextWithBackground( g, item );
		}
	}
}
