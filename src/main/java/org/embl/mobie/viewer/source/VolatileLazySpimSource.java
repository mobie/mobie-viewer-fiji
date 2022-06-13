package org.embl.mobie.viewer.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazySpimSource< N extends NumericType< N >, V extends Volatile< N > > extends AbstractLazySpimSource< N > implements Source< V >
{
	public VolatileLazySpimSource( LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables )
	{
		super( lazySourceAndConverterAndTables );
	}

	@Override
	public RandomAccessibleInterval< V > getSource( int t, int level )
	{
		return openVolatileSpimSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< V > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return openVolatileSpimSource().getInterpolatedSource( t, level, method );
	}

	@Override
	public V getType()
	{
		return openVolatileSpimSource().getType();
	}

	private Source< V > openVolatileSpimSource()
	{
		return (Source< V >) lazySourceAndConverterAndTables.openSourceAndConverter().asVolatile().getSpimSource();
	}
}

