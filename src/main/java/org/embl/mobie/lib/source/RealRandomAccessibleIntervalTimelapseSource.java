package org.embl.mobie.lib.source;

import bdv.util.RealRandomAccessibleSource;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;

import java.util.Set;

public class RealRandomAccessibleIntervalTimelapseSource< T extends Type< T > > extends RealRandomAccessibleSource< T >
{
	private final Interval interval;

	private final AffineTransform3D sourceTransform;
	private final Set< Integer > timePoints;

	public RealRandomAccessibleIntervalTimelapseSource(
			final RealRandomAccessible< T > accessible,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxIntersectionCheck,
			final Set< Integer > timePoints )
	{
		super( accessible, type, name, new DefaultVoxelDimensions( -1 ), doBoundingBoxIntersectionCheck );
		this.interval = interval;
		this.sourceTransform = sourceTransform;
		this.timePoints = timePoints;
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}

	@Override
	public Interval getInterval( final int t, final int level )
	{
		return interval;
	}

	@Override
	public boolean isPresent( final int t )
	{
		if ( timePoints == null )
			return t == 0;

		return timePoints.contains( t );
	}
}