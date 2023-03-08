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
package org.embl.mobie.lib.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;
import org.embl.mobie.lib.source.AnnotationType;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SourceNameOverlay extends BdvOverlay implements TransformListener< AffineTransform3D >
{
	private final BdvHandle bdvHandle;
	private Map< String, FinalRealInterval > sourceNameToBounds = new ConcurrentHashMap< String, FinalRealInterval >();
	private FinalRealInterval viewerInterval;
	private BdvOverlaySource< SourceNameOverlay > overlaySource;
	private boolean isActive;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, 20 );;

	public SourceNameOverlay( BdvHandle bdvHandle, boolean isActive )
	{
		this.bdvHandle = bdvHandle;
		bdvHandle.getViewerPanel().transformListeners().add( this );
		setActive( isActive );
	}

	public void setActive( boolean isActive )
	{
		this.isActive = isActive;

		if ( isActive && overlaySource == null )
		{
			// only add the overlay source once it is activated
			// otherwise it interferes from unknown reasons
			// with the scale bar overlay of BDV
			overlaySource = BdvFunctions.showOverlay(
					this,
					"sourceNameRenderer",
					BdvOptions.options().addTo( bdvHandle ) );
		}

		if ( overlaySource != null )
			overlaySource.setActive( isActive );
	}

	public boolean isActive()
	{
		return isActive;
	}

	@Override
	public void transformChanged( AffineTransform3D transform3D )
	{
		adaptSourceNames();
	}

	private synchronized void adaptSourceNames()
	{
		sourceNameToBounds.clear();

		final ViewerState viewerState = bdvHandle.getViewerPanel().state().snapshot();

		final AffineTransform3D viewerTransform = viewerState.getViewerTransform();

		viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		final Set< SourceAndConverter< ? > > sourceAndConverters = viewerState.getVisibleAndPresentSources();

		final int t = viewerState.getCurrentTimepoint();

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		final double[] sourceMin = new double[ 3 ];
		final double[] sourceMax = new double[ 3 ];

		for ( final SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final Source< ? > spimSource = sourceAndConverter.getSpimSource();
			final Object type = spimSource.getType();
			if ( type instanceof AnnotationType )
			{
				// don't render names of annotations
				// TODO: this right now also includes segmentations
				//   we could differentiate more by
				//   type.get() instance of Region...
				continue;
			}

			final int level = 0; // spimSource.getNumMipmapLevels() - 1;
			spimSource.getSourceTransform( t, level, sourceToGlobal );

			final Interval interval = spimSource.getSource( t, level );
			for ( int d = 0; d < 3; d++ )
			{
				sourceMin[ d ] = interval.realMin( d );
				sourceMax[ d ] = interval.realMax( d );
			}

			final FinalRealInterval globalBounds = sourceToGlobal.estimateBounds( new FinalRealInterval( sourceMin, sourceMax ) );
			final FinalRealInterval intersect = Intervals.intersect( globalBounds, viewerInterval );
			if ( ! Intervals.isEmpty( intersect ) )
			{
				// If we want the name to be always visible
				// we could do:
				// final FinalRealInterval boundsInViewer = viewerTransform.estimateBounds( intersect );
				final FinalRealInterval boundsInViewer = viewerTransform.estimateBounds( globalBounds );
				sourceNameToBounds.put( spimSource.getName(), boundsInViewer );
			}
		}
	}

	@Override
	protected void draw( Graphics2D g )
	{
		for ( Map.Entry< String, FinalRealInterval > entry : sourceNameToBounds.entrySet() )
		{
			// determine the size of the annotated source
			// in the viewer
			final FinalRealInterval bounds = entry.getValue();
			final double sourceWidth = bounds.realMax( 0 ) - bounds.realMin( 0 );
			final double sourceCenter = ( bounds.realMax( 0 ) + bounds.realMin( 0 ) ) / 2.0;

			// determine font of appropriate size
			final String name = entry.getKey();
			g.setFont( font );
			g.setColor( Color.WHITE );
			final float finalFontSize = Math.min ( font.getSize(), ( float ) ( 1.0F * font.getSize() * sourceWidth / ( 1.0F * g.getFontMetrics().stringWidth( name ) ) ) );
			Font finalFont = font.deriveFont( finalFontSize );
			g.setFont( finalFont );

			// draw the name below the source
			final float x = (float) ( sourceCenter - g.getFontMetrics().stringWidth( name ) / 2.0 );
			final float y = (float) bounds.realMax( 1 ) + 1.1F * finalFont.getSize();
			g.drawString( name, x, y );
		}
	}
}
