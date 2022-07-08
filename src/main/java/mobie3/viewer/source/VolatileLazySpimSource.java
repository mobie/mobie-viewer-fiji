package mobie3.viewer.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazySpimSource< N extends NumericType< N >, V extends Volatile< N > > extends AbstractLazySpimSource< N > implements Source< V >
{
	private boolean isOpen = false;
	private Source< V > vSource;

	public VolatileLazySpimSource( SourceAndConverterAndTables< N > sourceAndConverterAndTables )
	{
		super( sourceAndConverterAndTables );
	}

	@Override
	public RandomAccessibleInterval< V > getSource( int t, int level )
	{
		return getVolatileSpimSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< V > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return getVolatileSpimSource().getInterpolatedSource( t, level, method );
	}

	@Override
	public V getType()
	{
		return ( V ) getVolatileInitializationSource().getType();
	}

	private Source< V > getVolatileSpimSource()
	{
		if ( vSource == null )
		{
			open();
		}

		return vSource;
	}

	public synchronized void open()
	{
		if ( isOpen )
			return;

		vSource = ( Source< V > ) sourceAndConverterAndTables.getSourceAndConverter().asVolatile().getSpimSource();
		isOpen = true;
	}

	public boolean isOpen()
	{
		return isOpen;
	}
}

