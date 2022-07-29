package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.type.Type;


/**
 *  * This could be an example to work from (very similar to my use case):
 *  *
 *  * abstract public class AbstractVolatileNumericType< N extends NumericType< N >, T extends AbstractVolatileNumericType< N, T > >
 *  * 		extends Volatile< N >
 *  * 		implements NumericType< T >
 */

// Note: This must be a type (as in right now implementing Type< VolatileAnnotationType< T > > ) otherwise it cannot be a pixel in a Source
public class VolatileAnnotationType< T > extends Volatile< AnnotationType< T > > implements Type< VolatileAnnotationType< T > >
{

	public VolatileAnnotationType( T annotation, boolean valid )
	{
		super( new AnnotationType<>( annotation ), valid );
	}

	@Override
	public VolatileAnnotationType< T > createVariable()
	{
		return new VolatileAnnotationType<>( null, true );
	}

	@Override
	public VolatileAnnotationType< T > copy()
	{
		final VolatileAnnotationType< T > volatileAnnotationType = createVariable();
		volatileAnnotationType.set( this );
		return volatileAnnotationType;
	}

	@Override
	public void set( VolatileAnnotationType< T > c )
	{
		valid = c.isValid();
		t.set( c.get() );
	}

	@Override
	public boolean valueEquals( VolatileAnnotationType< T > va )
	{
		return t.valueEquals( va.t );
	}

	public T getAnnotation()
	{
		return get().getAnnotation();
	}
}
