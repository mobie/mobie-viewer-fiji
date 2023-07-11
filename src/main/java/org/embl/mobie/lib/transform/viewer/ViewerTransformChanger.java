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
package org.embl.mobie.lib.transform.viewer;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import bdv.viewer.animate.SimilarityTransformAnimator;
import org.embl.mobie.lib.bdv.CircleOverlay;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.TransformHelper;

import java.util.Arrays;

public abstract class ViewerTransformChanger
{
	public static int animationDurationMillis = 1500;

	private static BdvOverlaySource< BdvOverlay > pointOverlaySource;
	private static CircleOverlay circleOverlay;
	private static boolean pointOverlaySourceIsActive;
	private static boolean isPointOverlayEnabled;

	public static void changeLocation( BdvHandle bdvHandle, ViewerTransform viewerTransform )
	{
		if ( viewerTransform instanceof PositionViewerTransform )
		{
			moveToPosition( bdvHandle, viewerTransform.getParameters(), animationDurationMillis );
			adaptTimepoint( bdvHandle, viewerTransform );
			if ( isPointOverlayEnabled )
				addPointOverlay( bdvHandle, viewerTransform.getParameters() );
		}
		else if ( viewerTransform instanceof TimepointViewerTransform )
		{
			adaptTimepoint( bdvHandle, viewerTransform );
		}
		else if ( viewerTransform instanceof NormalVectorViewerTransform )
		{
			final AffineTransform3D transform = NormalVectorViewerTransform.createTransform( bdvHandle, viewerTransform.getParameters() );
			changeLocation( bdvHandle, transform, animationDurationMillis );
			adaptTimepoint( bdvHandle, viewerTransform );
		}
		else if ( viewerTransform instanceof AffineViewerTransform )
		{
			changeLocation( bdvHandle, TransformHelper.asAffineTransform3D( viewerTransform.getParameters() ), animationDurationMillis );
			adaptTimepoint( bdvHandle, viewerTransform );
		}
		else if ( viewerTransform instanceof NormalizedAffineViewerTransform )
		{
			final AffineTransform3D transform = TransformHelper.createUnnormalizedViewerTransform( TransformHelper.asAffineTransform3D( viewerTransform.getParameters() ), bdvHandle.getBdvHandle().getViewerPanel() );
			changeLocation( bdvHandle, transform, animationDurationMillis );
			adaptTimepoint( bdvHandle, viewerTransform );
		}
	}

	public static void adaptTimepoint( BdvHandle bdvHandle, ViewerTransform viewerTransform )
	{
		if ( viewerTransform.getTimepoint() != null )
			bdvHandle.getViewerPanel().setTimepoint( viewerTransform.getTimepoint() );
	}

	public static void togglePointOverlay()
	{
		if ( pointOverlaySource == null ) return;

		pointOverlaySourceIsActive = ! pointOverlaySourceIsActive;
		pointOverlaySource.setActive( pointOverlaySourceIsActive );
	}

	private static void addPointOverlay( BdvHandle bdvHandle, double[] doubles )
	{
		if ( circleOverlay == null )
		{
			circleOverlay = new CircleOverlay( doubles, 5.0 );
			pointOverlaySource = BdvFunctions.showOverlay(
					circleOverlay,
					"point-overlay-" + Arrays.toString( doubles ),
					BdvOptions.options().addTo( bdvHandle ) );
			pointOverlaySourceIsActive = true;
		}
		else
		{
			circleOverlay.addCircle( doubles );
		}
	}

	public static void enablePointOverlay( boolean isPointOverlayEnabled )
	{
		ViewerTransformChanger.isPointOverlayEnabled = isPointOverlayEnabled;
	}

	public static void moveToPosition( BdvHandle bdvHandle, double[] xyz, long durationMillis )
	{
		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D newViewerTransform = currentViewerTransform.copy();

		// ViewerTransform
		// applyInverse: coordinates in viewer => coordinates in image
		// apply: coordinates in image => coordinates in viewer

		final double[] locationOfTargetCoordinatesInCurrentViewer = new double[ 3 ];
		currentViewerTransform.apply( xyz, locationOfTargetCoordinatesInCurrentViewer );

		for ( int d = 0; d < 3; d++ )
		{
			locationOfTargetCoordinatesInCurrentViewer[ d ] *= -1;
		}

		newViewerTransform.translate( locationOfTargetCoordinatesInCurrentViewer );
		final double[] bdvWindowCenter = BdvPlaygroundHelper.getWindowCentreInPixelUnits( bdvHandle.getViewerPanel() );
		newViewerTransform.translate( bdvWindowCenter );

		if ( durationMillis <= 0 )
		{
			bdvHandle.getBdvHandle().getViewerPanel().state().setViewerTransform(  newViewerTransform );
		}
		else
		{
			final SimilarityTransformAnimator similarityTransformAnimator =
					new SimilarityTransformAnimator(
							currentViewerTransform,
							newViewerTransform,
							0,
							0,
							durationMillis );

			bdvHandle.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		}
	}

	public static void changeLocation( BdvHandle bdvHandle, AffineTransform3D newViewerTransform, long duration)
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0 ,
						0,
						duration );

		bdvHandle.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
	}

}
