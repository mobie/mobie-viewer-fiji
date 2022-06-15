package org.embl.mobie.viewer.source;


import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.transform.RealIntervalProvider;

public class LazySpimSource< N extends NumericType< N > > extends AbstractLazySpimSource< N > implements Source< N >
{
	public LazySpimSource( SourceAndConverterAndTables< N > sourceAndConverterAndTables )
	{
		super( sourceAndConverterAndTables );
	}

	@Override
	public RandomAccessibleInterval< N > getSource( int t, int level )
	{
		return openSpimSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< N > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return openSpimSource().getInterpolatedSource( t, level, method );
	}

	public N getType()
	{
		return getInitializationSource().getType();
	}

	private Source< N > openSpimSource()
	{
		return sourceAndConverterAndTables.getSourceAndConverter().getSpimSource();
	}

}

