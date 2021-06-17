package de.embl.cba.mobie.transform;

import bdv.util.*;
import bdv.viewer.animate.SimilarityTransformAnimator;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.PlaygroundUtils;
import de.embl.cba.mobie.bdv.BdvPointOverlay;
import de.embl.cba.mobie.Utils;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;

public abstract class ViewerTransformChanger
{
	public static int animationDurationMillis = 3000;

	private static BdvOverlaySource< BdvOverlay > pointOverlaySource;
	private static BdvPointOverlay bdvPointOverlay;
	private static boolean pointOverlaySourceIsActive;
	private static boolean isPointOverlayEnabled;

	public static void changeViewerTransform( BdvHandle bdv, ViewerTransform viewerTransform )
	{
		if ( viewerTransform instanceof PositionViewerTransform )
		{
			moveToPosition( bdv, viewerTransform.getParameters(), animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
			if ( isPointOverlayEnabled )
				addPointOverlay( bdv, viewerTransform.getParameters() );
		}
		else if ( viewerTransform instanceof TimepointViewerTransform )
		{
			adaptTimepoint( bdv, viewerTransform );
		}
		else if ( viewerTransform instanceof AffineViewerTransform )
		{
			// TODO: changing time point and viewer transform at the same time
			//  used to sometimes lead to hickups; check whether this is the case and think about a solution...
			//  e.g., does changeBdvViewerTransform return already while the transformation is still going on?
			changeViewerTransform( bdv, Utils.asAffineTransform3D( viewerTransform.getParameters() ), animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
		}
		else if ( viewerTransform instanceof NormalizedAffineViewerTransform )
		{
			final AffineTransform3D transform = Utils.createUnnormalizedViewerTransform( Utils.asAffineTransform3D( viewerTransform.getParameters() ), bdv );
			changeViewerTransform( bdv, transform, animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
		}
	}

	private static void adaptTimepoint( BdvHandle bdv, ViewerTransform viewerTransform )
	{
		if ( viewerTransform.getTimepoint() != null )
			bdv.getViewerPanel().setTimepoint( viewerTransform.getTimepoint() );
	}

	public static void togglePointOverlay()
	{
		if ( pointOverlaySource == null ) return;

		pointOverlaySourceIsActive = ! pointOverlaySourceIsActive;
		pointOverlaySource.setActive( pointOverlaySourceIsActive );
	}

	private static void addPointOverlay( Bdv bdv, double[] doubles )
	{
		if ( bdvPointOverlay == null )
		{
			bdvPointOverlay = new BdvPointOverlay( doubles, 5.0 );
			pointOverlaySource = BdvFunctions.showOverlay(
					bdvPointOverlay,
					"point-overlay-" + Arrays.toString( doubles ),
					BdvOptions.options().addTo( bdv ) );
			pointOverlaySourceIsActive = true;
		}
		else
		{
			bdvPointOverlay.addPoint( doubles );
		}
	}

	public static void enablePointOverlay( boolean isPointOverlayEnabled )
	{
		ViewerTransformChanger.isPointOverlayEnabled = isPointOverlayEnabled;
	}

	public static void moveToPosition( BdvHandle bdv, double[] xyz, long durationMillis )
	{
		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

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
		final double[] bdvWindowCenter = PlaygroundUtils.getWindowCentreInPixelUnits( bdv );
		newViewerTransform.translate( bdvWindowCenter );

		if ( durationMillis <= 0 )
		{
			bdv.getBdvHandle().getViewerPanel().state().setViewerTransform(  newViewerTransform );
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

			bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		}
	}

	public static void changeViewerTransform( Bdv bdv, AffineTransform3D newViewerTransform, long duration)
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0 ,
						0,
						duration );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
	}

}
