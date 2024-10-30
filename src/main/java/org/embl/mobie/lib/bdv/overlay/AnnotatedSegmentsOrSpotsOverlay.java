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
import bdv.viewer.TransformListener;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.view.SliceViewer;

import java.awt.*;
import java.util.ArrayList;

public class AnnotatedSegmentsOrSpotsOverlay< A extends Annotation >
		extends BdvOverlay implements AnnotationOverlay, TransformListener< AffineTransform3D >
{
	private final SliceViewer sliceViewer;
	private final ArrayList< A > annotations;
	private final ArrayList< OverlayItem > overlayItems;
	private final String annotationColumn;
	private BdvOverlaySource< AnnotatedSegmentsOrSpotsOverlay > overlaySource;
	public static final int MAX_FONT_SIZE = 20;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, MAX_FONT_SIZE );
	private AffineTransform3D viewerTransform;

	private long start;

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

		this.overlayItems = new ArrayList<>();
		this.viewerTransform = sliceViewer.getBdvHandle().getViewerPanel().state().getViewerTransform();
		sliceViewer.getBdvHandle().getViewerPanel().transformListeners().add( this );

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
	protected synchronized void draw( Graphics2D g )
	{
		if ( annotationColumn == null || annotations == null || annotations.size() == 0 )
			return;

		if ( viewerTransform != null )
		{
			start = System.currentTimeMillis();
			updateOverlayItems( g );
			viewerTransform = null;
			// System.out.println( "updated " + annotations.size() + " annotations in [ms] " + ( System.currentTimeMillis() - start ) );
		}

		start = System.currentTimeMillis();
		g.setColor( Color.WHITE ); // TODO make the color configurable
		for ( OverlayItem overlayItem : overlayItems )
		{
			g.drawString( overlayItem.text, overlayItem.x, overlayItem.y );
		}
		// System.out.println( "drawn " + overlayItems.size() +  " overlay items in [ms] " + ( System.currentTimeMillis() - start ) );
	}

	private void updateOverlayItems( Graphics2D g )
	{
		BdvHandle bdvHandle = sliceViewer.getBdvHandle();
		int width = bdvHandle.getViewerPanel().getWidth();
		int height = bdvHandle.getViewerPanel().getHeight();

		double[] globalPosition = new double[ 3 ];

		this.overlayItems.clear();

		for ( A annotation : annotations )
		{
			double[] viewPosition = new double[ 3 ];

			annotation.localize( globalPosition );
			viewerTransform.apply( globalPosition, viewPosition );

			if ( viewPosition[ 0 ] < 0
					|| viewPosition[ 1 ] < 0
					|| viewPosition[ 0 ] > width
					|| viewPosition[ 1 ] > height
					|| Math.abs( viewPosition[ 2 ] ) > 15 // TODO this is a bit random...
			)
				continue;

			// changing the font size turns out to be expensive during rendering,
			// thus we don't do it
			//float fontSize = ( float ) ( MAX_FONT_SIZE - Math.abs( viewPosition[ 2 ] ) );
			//if ( fontSize < 2 )
			//	continue;

			OverlayItem overlayItem = new OverlayItem();
			//overlayItem.font = font.deriveFont( Math.min( MAX_FONT_SIZE, fontSize ) );
			overlayItem.text = annotation.getValue( annotationColumn ).toString();
			//g.setFont( font.deriveFont( Math.min( MAX_FONT_SIZE, fontSize ) ) );
			overlayItem.width = g.getFontMetrics().stringWidth( overlayItem.text );
			overlayItem.height = g.getFontMetrics().getHeight();
			overlayItem.x = ( int ) ( viewPosition[ 0 ] - overlayItem.width / 2 );
			overlayItem.y = ( int ) ( viewPosition[ 1 ] + 1.5 * overlayItem.height ); // paint a bit below (good for points)

			overlayItems.add( overlayItem );
		}
	}

	@Override
	public void transformChanged( AffineTransform3D transform )
	{
		this.viewerTransform = transform;
	}
}
