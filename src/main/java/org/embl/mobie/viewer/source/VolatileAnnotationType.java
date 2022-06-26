package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.type.Type;
import net.imglib2.type.volatiles.VolatileARGBType;


/**
 *  * This could be an example to work from (very similar to my use case):
 *  *
 *  * abstract public class AbstractVolatileNumericType< N extends NumericType< N >, T extends AbstractVolatileNumericType< N, T > >
 *  * 		extends Volatile< N >
 *  * 		implements NumericType< T >
 *
 * @param <A>
 */
// Do we need an interface for AnnotationType?

public class VolatileAnnotationType< A extends AnnotationType< A >, VA extends VolatileAnnotationType< A, VA > > extends Volatile< A > implements Type< VA >
{

	@Override
	public VA createVariable()
	{
		return null;
	}

	@Override
	public VA copy()
	{
		return null;
	}

	@Override
	public void set( VA c )
	{

	}

	@Override
	public boolean valueEquals( VA va )
	{
		return false;
	}
}
