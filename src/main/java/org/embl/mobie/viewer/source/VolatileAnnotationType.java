package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;
import net.imglib2.Volatile;
import net.imglib2.type.Type;


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

public class VolatileAnnotationType< T > extends Volatile< AnnotationType< T > > implements Type< VolatileAnnotationType< T > >
{
	public VolatileAnnotationType()
	{
		super( new AnnotationType<>(), true );
	}

	public VolatileAnnotationType( T annotation, boolean valid )
	{
		super( new AnnotationType<>( annotation ), valid );
	}

	@Override
	public VolatileAnnotationType< T > createVariable()
	{
		return null;
	}

	@Override
	public VolatileAnnotationType< T > copy()
	{
		return null;
	}

	@Override
	public void set( VolatileAnnotationType< T > c )
	{

	}

	@Override
	public boolean valueEquals( VolatileAnnotationType< T > va )
	{
		return false;
	}

	public T getAnnotation()
	{
		return get().getAnnotation();
	}
}
