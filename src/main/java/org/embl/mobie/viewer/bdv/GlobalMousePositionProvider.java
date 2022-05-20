package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import net.imglib2.RealPoint;

public class GlobalMousePositionProvider
{
	private final RealPoint realPoint;
	private final int timePoint;

	public GlobalMousePositionProvider( BdvHandle bdvHandle )
	{
		realPoint = new RealPoint( 3 );
		bdvHandle.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( realPoint );
		timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
	}

	public RealPoint getPositionAsRealPoint()
	{
		return realPoint;
	}

	public double[] getPositionAsDoubles()
	{
		double[] doubles = new double[ 3 ];
		realPoint.localize( doubles );
		return doubles;
	}

	public int getTimePoint()
	{
		return timePoint;
	}
}
