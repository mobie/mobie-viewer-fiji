package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.type.Type;


// TODO: this does not seem right as this is not coupled to AnnotationType at all!
public abstract class VolatileAnnotationType< T > extends Volatile< T > implements Type< VolatileAnnotationType< T > >
{
	public VolatileAnnotationType( T t, boolean valid )
	{
		super( t, valid );
	}

	public VolatileAnnotationType( T t )
	{
		super( t );
	}
}
