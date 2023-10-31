/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.image.StitchedImage;
import org.embl.mobie.lib.select.Listeners;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImageNameOverlay extends BdvOverlay implements TransformListener< AffineTransform3D >
{
	private final BdvHandle bdvHandle;
	private final SliceViewer sliceViewer;
	private Map< String, FinalRealInterval > nameToBounds = new ConcurrentHashMap< String, FinalRealInterval >();
	private BdvOverlaySource< ImageNameOverlay > overlaySource;
	private boolean isActive;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, 20 );

	protected final Listeners.SynchronizedList< ActiveListener > listeners
			= new Listeners.SynchronizedList< ActiveListener >(  );


	public ImageNameOverlay( BdvHandle bdvHandle, boolean isActive, SliceViewer sliceViewer )
	{
		this.bdvHandle = bdvHandle;
		this.sliceViewer = sliceViewer;
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
					"sourceNameOverlay",
					BdvOptions.options().addTo( bdvHandle ) );

			// This is needed probably due a bug:
			// https://imagesc.zulipchat.com/#narrow/stream/327326-BigDataViewer/topic/BdvOverlay.20and.20Timepoints
			// https://github.com/mobie/mobie-viewer-fiji/issues/976
			sliceViewer.updateTimepointSlider();
		}

		if ( overlaySource != null )
		{
			overlaySource.setActive( isActive );
		}

		for ( ActiveListener activeListener : listeners.list )
		{
			activeListener.isActive( isActive );
		}
	}

	public boolean isActive()
	{
		return isActive;
	}

	@Override
	public void transformChanged( AffineTransform3D transform3D )
	{
		if ( isActive )
		{
			adaptImageNames();
		}
	}

	private synchronized void adaptImageNames()
	{
		nameToBounds.clear();

		final ViewerState viewerState = bdvHandle.getViewerPanel().state().snapshot();

		final AffineTransform3D viewerTransform = viewerState.getViewerTransform();

		FinalRealInterval viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		final Set< SourceAndConverter< ? > > sourceAndConverters = viewerState.getVisibleAndPresentSources();

		for ( final SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			Image< ? > image = DataStore.sourceToImage().get( sourceAndConverter );

			if ( image instanceof RegionAnnotationImage )
			{
				continue;
			}

			if ( image instanceof StitchedImage )
			{
				final List< ? extends Image< ? > > tileImages = ( ( StitchedImage< ?, ? > ) image ).getTileImages();

				for ( Image< ? > tileImage : tileImages )
				{
					addImageToOverlay( viewerTransform, viewerInterval, tileImage );
				}

				continue;
			}

			addImageToOverlay( viewerTransform, viewerInterval, image );
		}
	}

	private void addImageToOverlay( AffineTransform3D viewerTransform, FinalRealInterval viewerInterval, Image< ? > image )
	{
		final RealMaskRealInterval imageMask = image.getMask();
		final FinalRealInterval intersect = Intervals.intersect( viewerInterval, imageMask );
		if ( ! Intervals.isEmpty( intersect ) )
		{
			nameToBounds.put( image.getName(), viewerTransform.estimateBounds( imageMask ) );
		}
	}

	@Override
	protected void draw( Graphics2D g )
	{
		for ( Map.Entry< String, FinalRealInterval > entry : nameToBounds.entrySet() )
		{
			// determine the size of the annotated source
			// in the viewer
			final FinalRealInterval bounds = entry.getValue();
			final double sourceWidth = bounds.realMax( 0 ) - bounds.realMin( 0 );
			final double sourceCenter = ( bounds.realMax( 0 ) + bounds.realMin( 0 ) ) / 2.0;

			// determine appropriate font
			final String name = entry.getKey();
			g.setFont( font );
			g.setColor( Color.WHITE );
			final float finalFontSize = Math.min ( font.getSize(), ( float ) ( 1.0F * font.getSize() * sourceWidth / ( 1.0F * g.getFontMetrics().stringWidth( name ) ) ) );
			Font finalFont = font.deriveFont( finalFontSize );
			g.setFont( finalFont );

			// draw name below image
			final float x = (float) ( sourceCenter - g.getFontMetrics().stringWidth( name ) / 2.0 );
			final float y = (float) bounds.realMax( 1 ) + 1.1F * finalFont.getSize();
			g.drawString( name, x, y );
		}
	}

	public void addListener( ActiveListener activeListener )
	{
		listeners.add( activeListener );
	}
}
