package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.type.Type;

// Note: This must be a type (as in right now implementing Type< VolatileAnnotationType< T > > ) otherwise it cannot be a pixel in a Source
public class VolatileAnnotationType< T > extends Volatile< AnnotationType< T > > implements Type< VolatileAnnotationType< T > >
{
	public VolatileAnnotationType( )
	{
		super( new AnnotationType<>( null ), false );
	}

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
