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
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.bdv.ActiveListener;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.image.StitchedImage;
import org.embl.mobie.lib.select.Listeners;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ImageNameOverlay extends BdvOverlay implements TransformListener< AffineTransform3D >
{
	private final BdvHandle bdvHandle;
	private final SliceViewer sliceViewer;
	private BdvOverlaySource< ImageNameOverlay > overlaySource;

	private List< OverlayItem > overlayItems = new ArrayList<>();
	private boolean isActive;
	public static final int MAX_FONT_SIZE = 20;
	private static final Font font = new Font( "Monospaced", Font.PLAIN, MAX_FONT_SIZE );

	protected final Listeners.SynchronizedList< ActiveListener > activeListeners
			= new Listeners.SynchronizedList< >(  );

	private AffineTransform3D viewerTransform;

	public ImageNameOverlay( SliceViewer sliceViewer )
	{
		this.bdvHandle = sliceViewer.getBdvHandle();
		this.sliceViewer = sliceViewer;
		bdvHandle.getViewerPanel().transformListeners().add( this );
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

		for ( ActiveListener activeListener : activeListeners.list )
		{
			activeListener.isActive( isActive );
		}

		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void transformChanged( AffineTransform3D transform3D )
	{
		this.viewerTransform = transform3D;
	}

	private void updateOverlayItems( Graphics2D g )
	{
		overlayItems.clear();

		final ViewerState viewerState = bdvHandle.getViewerPanel().state().snapshot();

		final AffineTransform3D viewerTransform = viewerState.getViewerTransform();

		FinalRealInterval viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );


		final Set< SourceAndConverter< ? > > sourceAndConverters = viewerState.getVisibleAndPresentSources();

		for ( final SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			Image< ? > image = DataStore.sourceToImage().get( sourceAndConverter );

			if ( image instanceof RegionAnnotationImage )
			{
				// Do nothing
			}
			else if ( image instanceof StitchedImage )
			{
				final List< ? extends Image< ? > > tileImages = ( ( StitchedImage< ?, ? > ) image ).getTileImages();

				for ( Image< ? > tileImage : tileImages )
				{
					final RealMaskRealInterval imageMask = tileImage.getMask();
					final FinalRealInterval intersect = Intervals.intersect( viewerInterval, imageMask );
					if ( ! Intervals.isEmpty( intersect ) )
					{
						createAndAddNewOverlayItem( g, viewerTransform, imageMask, image );
					}
				}
			}
			else
			{
				final RealMaskRealInterval imageMask = image.getMask();
				final FinalRealInterval intersect = Intervals.intersect( viewerInterval, imageMask );
				if ( !Intervals.isEmpty( intersect ) )
				{
					createAndAddNewOverlayItem( g, viewerTransform, imageMask, image );
				}
			}
		}
	}

	private void createAndAddNewOverlayItem( Graphics2D g, AffineTransform3D viewerTransform, RealMaskRealInterval imageMask, Image< ? > image )
	{
		OverlayItem newItem = OverlayHelper.itemFromBounds(
				g,
				viewerTransform.estimateBounds( imageMask ),
				image.getName(),
				g.getFont() );

		boolean addItem = true;
		for ( OverlayItem item : overlayItems )
		{
			// TODO: This could be changed such that small overlaps are tolerated
			if ( ! Intervals.isEmpty( Intervals.intersect( item.interval, newItem.interval ) ) )
			{
				addItem = false;
				break;
			}
		}

		if ( addItem )
			overlayItems.add( newItem );
	}

	@Override
	protected synchronized void draw( Graphics2D g )
	{
		if ( viewerTransform != null )
		{
			updateOverlayItems( g );
			viewerTransform = null;
		}

		for ( OverlayItem overlayItem : overlayItems )
		{
			OverlayHelper.drawTextWithBackground( g, overlayItem, false );
		}
	}

	public void addListener( ActiveListener activeListener )
	{
		activeListeners.add( activeListener );
	}
}
