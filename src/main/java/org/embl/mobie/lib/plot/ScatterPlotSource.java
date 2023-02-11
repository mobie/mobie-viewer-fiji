package org.embl.mobie.lib.plot;

import bdv.util.RealRandomAccessibleSource;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.Type;

public class ScatterPlotSource< T extends Type< T > > extends RealRandomAccessibleSource< T >
{
	private final Interval interval;

	public ScatterPlotSource( RealRandomAccessible< T > accessible, Interval interval, T type, String name, VoxelDimensions voxelDimensions )
	{
		super( accessible, type, name, voxelDimensions );
		this.interval = interval;
	}

	@Override
	public Interval getInterval( int t, int level )
	{
		return interval;
	}
}
