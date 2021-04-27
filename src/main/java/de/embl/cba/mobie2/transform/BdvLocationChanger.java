package de.embl.cba.mobie2.transform;

import bdv.util.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.bdv.BdvPointOverlay;
import de.embl.cba.mobie.utils.Utils;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;

public abstract class BdvLocationChanger
{
	public static int animationDurationMillis = 3000;

	private static BdvOverlaySource< BdvOverlay > pointOverlaySource;
	private static BdvPointOverlay bdvPointOverlay;
	private static boolean pointOverlaySourceIsActive;
	private static boolean isPointOverlayEnabled;

	public static void moveToLocation( BdvHandle bdv, BdvLocation bdvLocation )
	{
		switch ( bdvLocation.type )
		{
			case Position3d:

				BdvUtils.moveToPosition( bdv, bdvLocation.doubles, 0, animationDurationMillis );

				if ( isPointOverlayEnabled )
					addPointOverlay( bdv, bdvLocation.doubles );
				break;

			case Position3dAndTime:

				final double[] position = new double[ 3 ];
				for ( int d = 0; d < 3; d++ )
					position[ d ] = bdvLocation.doubles[ d ];
				final int time = (int) bdvLocation.doubles[ 3 ];

				BdvUtils.moveToPosition( bdv, position, time, animationDurationMillis );
				break;

			case ViewerTransform:

				BdvUtils.changeBdvViewerTransform( bdv, Utils.asAffineTransform3D( bdvLocation.doubles ), animationDurationMillis );
				break;

			case NormalisedViewerTransform:

				final AffineTransform3D transform = Utils.createUnnormalizedViewerTransform( Utils.asAffineTransform3D( bdvLocation.doubles ), bdv );
				BdvUtils.changeBdvViewerTransform( bdv, transform, animationDurationMillis );
				break;
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
		BdvLocationChanger.isPointOverlayEnabled = isPointOverlayEnabled;
	}
}
