package org.embl.mobie.viewer.playground;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;


/**
 * These helper functions either exist already in bdv-playground,
 * but in a too recent version, or should be moved to bdv-playground.
 */
public class PlaygroundUtils
{
	public static double[] getWindowCentreInPixelUnits( BdvHandle bdvHandle )
	{
		final double[] windowCentreInPixelUnits = new double[ 3 ];
		windowCentreInPixelUnits[ 0 ] = bdvHandle.getViewerPanel().getDisplay().getWidth() / 2.0;
		windowCentreInPixelUnits[ 1 ] = bdvHandle.getViewerPanel().getDisplay().getHeight() / 2.0;
		return windowCentreInPixelUnits;
	}

	public static double[] getWindowCentreInCalibratedUnits( BdvHandle bdvHandle )
	{
		final double[] centreInPixelUnits = getWindowCentreInPixelUnits( bdvHandle );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( affineTransform3D );
		final double[] centreInCalibratedUnits = new double[ 3 ];
		affineTransform3D.inverse().apply( centreInPixelUnits, centreInCalibratedUnits );
		return centreInCalibratedUnits;
	}

}
