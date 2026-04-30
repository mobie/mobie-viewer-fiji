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

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import net.imglib2.FinalInterval;

import org.embl.mobie.lib.bdv.PixelValueAtMouseSupplier;
import org.embl.mobie.lib.bdv.view.SliceViewer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PixelValueOverlay extends BdvOverlay
{
	private static final int MAX_LINES = 20;
	private static final int RIGHT_PADDING = 12;
	private static final int BOTTOM_PADDING = 20;
	private static final int REPAINT_MARGIN = 6;
	private static final Font FONT = new Font( "Monospaced", Font.PLAIN, 14 );

	private final BdvHandle bdvHandle;
	private final SliceViewer sliceViewer;
	private final PixelValueAtMouseSupplier pixelValueAtMouseSupplier;
	private final Timer repaintTimer;
	private volatile FinalInterval currentOverlayInterval;
	private volatile FinalInterval repaintInterval;

	private BdvOverlaySource< PixelValueOverlay > overlaySource;
	private boolean isActive;

	public PixelValueOverlay( SliceViewer sliceViewer )
	{
		this.sliceViewer = sliceViewer;
		this.bdvHandle = sliceViewer.getBdvHandle();
		this.pixelValueAtMouseSupplier = new PixelValueAtMouseSupplier( bdvHandle, sliceViewer.is2D() );
		this.repaintTimer = new Timer( 120, e -> safeRepaintOverlayRegion() );
		this.repaintTimer.setCoalesce( true );
	}

	public boolean isActive()
	{
		return isActive;
	}

	public void setActive( boolean isActive )
	{
		if ( this.isActive == isActive )
			return;

		this.isActive = isActive;

		if ( isActive && overlaySource == null )
		{
			overlaySource = BdvFunctions.showOverlay(
					this,
					"pixelValueOverlay",
					BdvOptions.options().addTo( bdvHandle ) );

			// Workaround for BDV overlay-timepoint interactions.
			sliceViewer.updateTimepointSlider();
		}

		if ( overlaySource != null )
			overlaySource.setActive( isActive );

		if ( isActive )
			repaintTimer.start();
		else
			repaintTimer.stop();

		safeRepaintOverlayRegion();
	}

	@Override
	protected synchronized void draw( Graphics2D g )
	{
		if ( !isViewerAvailable() )
		{
			repaintTimer.stop();
			return;
		}

		g.setFont( FONT );
		final List< String > lines;
		try
		{
			lines = pixelValueAtMouseSupplier.get();
		}
		catch ( NullPointerException ignored )
		{
			// BDV may be disposed while timer/events are still draining.
			repaintTimer.stop();
			return;
		}

		final FontMetrics fontMetrics = g.getFontMetrics();
		final int lineHeight = fontMetrics.getHeight();
		final int numLines = Math.min( lines.size(), MAX_LINES );
		final boolean hasMoreLine = lines.size() > MAX_LINES;
		final int renderedLines = numLines + ( hasMoreLine ? 1 : 0 );

		int maxLineWidth = 0;
		for ( int i = 0; i < numLines; i++ )
		{
			maxLineWidth = Math.max( maxLineWidth, fontMetrics.stringWidth( lines.get( i ) ) );
		}
		if ( hasMoreLine )
		{
			maxLineWidth = Math.max( maxLineWidth, fontMetrics.stringWidth( "+" + ( lines.size() - MAX_LINES ) + " more" ) );
		}

		final int displayWidth = bdvHandle.getViewerPanel().getDisplayComponent().getWidth();
		final int displayHeight = bdvHandle.getViewerPanel().getDisplayComponent().getHeight();
		if ( displayWidth <= 0 || displayHeight <= 0 )
			return;

		final int blockHeight = renderedLines * lineHeight;
		final int blockLeft = Math.max( 0, displayWidth - RIGHT_PADDING - maxLineWidth );
		final int blockTop = Math.max( 0, displayHeight - BOTTOM_PADDING - blockHeight );

		int y = blockTop + fontMetrics.getAscent();
		for ( int i = 0; i < numLines; i++ )
		{
			final String line = lines.get( i );
			final int x = blockLeft + ( maxLineWidth - fontMetrics.stringWidth( line ) );
			drawLineWithOutline( g, line, x, y );
			y += lineHeight;
		}

		if ( hasMoreLine )
		{
			final String moreLine = "+" + ( lines.size() - MAX_LINES ) + " more";
			final int x = blockLeft + ( maxLineWidth - fontMetrics.stringWidth( moreLine ) );
			drawLineWithOutline( g, moreLine, x, y );
		}

		final int minX = Math.max( 0, blockLeft - REPAINT_MARGIN );
		final int minY = Math.max( 0, blockTop - REPAINT_MARGIN );
		final int maxX = Math.min( displayWidth - 1, blockLeft + maxLineWidth + REPAINT_MARGIN );
		final int maxY = Math.min( displayHeight - 1, blockTop + blockHeight + REPAINT_MARGIN );
		final FinalInterval newInterval = new FinalInterval( new long[] { minX, minY }, new long[] { maxX, maxY } );

		final FinalInterval oldInterval = currentOverlayInterval;
		currentOverlayInterval = newInterval;
		repaintInterval = oldInterval == null ? newInterval : union( oldInterval, newInterval );
	}

	private void drawLineWithOutline( Graphics2D g, String line, int x, int y )
	{
		g.setColor( Color.BLACK );
		g.drawString( line, x + 1, y + 1 );

		g.setColor( Color.WHITE );
		g.drawString( line, x, y );
	}

	private void safeRepaintOverlayRegion()
	{
		if ( !isViewerAvailable() )
		{
			repaintTimer.stop();
			return;
		}

		final FinalInterval interval = repaintInterval != null ? repaintInterval : currentOverlayInterval;
		if ( interval != null )
		{
			bdvHandle.getViewerPanel().requestRepaint( interval );
			repaintInterval = currentOverlayInterval;
		}
		else
		{
			bdvHandle.getViewerPanel().requestRepaint();
		}
	}

	private FinalInterval union( FinalInterval a, FinalInterval b )
	{
		final long minX = Math.min( a.min( 0 ), b.min( 0 ) );
		final long minY = Math.min( a.min( 1 ), b.min( 1 ) );
		final long maxX = Math.max( a.max( 0 ), b.max( 0 ) );
		final long maxY = Math.max( a.max( 1 ), b.max( 1 ) );
		return new FinalInterval( new long[] { minX, minY }, new long[] { maxX, maxY } );
	}

	private boolean isViewerAvailable()
	{
		return bdvHandle != null
				&& bdvHandle.getViewerPanel() != null
				&& bdvHandle.getViewerPanel().state() != null;
	}
}