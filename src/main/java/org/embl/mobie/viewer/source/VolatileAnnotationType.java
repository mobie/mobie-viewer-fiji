package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.type.Type;

public abstract class VolatileAnnotationType< T > extends Volatile< T > implements Type< VolatileAnnotationType< T > >, Annotation< T >
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
