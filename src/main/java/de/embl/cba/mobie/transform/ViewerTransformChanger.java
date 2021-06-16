package de.embl.cba.mobie.transform;

import bdv.util.*;
import de.embl.cba.bdv.utils.BdvUtils;
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
			BdvUtils.moveToPosition( bdv, viewerTransform.getParameters(), 0, animationDurationMillis );
			if ( isPointOverlayEnabled )
				addPointOverlay( bdv, viewerTransform.getParameters() );
		}
		else if ( viewerTransform instanceof AffineViewerTransform )
		{
			BdvUtils.changeBdvViewerTransform( bdv, Utils.asAffineTransform3D( viewerTransform.getParameters() ), animationDurationMillis );
		}
		else if ( viewerTransform instanceof NormalizedAffineViewerTransform )
		{
			final AffineTransform3D transform = Utils.createUnnormalizedViewerTransform( Utils.asAffineTransform3D( viewerTransform.getParameters() ), bdv );
			BdvUtils.changeBdvViewerTransform( bdv, transform, animationDurationMillis );
		}
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
}
