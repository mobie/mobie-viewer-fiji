package org.embl.mobie.lib.source;

import bdv.util.volatiles.VolatileTypeMatcher;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;

public class MoBIEVolatileTypeMatcher
{
	public static < T extends Type< T > > Type< ? > getVolatileTypeForType( final T type )
	{
		if ( type instanceof NativeType )
		{
			final NativeType< ? > volatileType = VolatileTypeMatcher.getVolatileTypeForType( (NativeType) type );
			return volatileType;
		}
		else if ( type instanceof AnnotationType )
		{
			return new VolatileAnnotationType<>( );
		}
		else
		{
			throw new UnsupportedOperationException("Cannot determine volatile type for " + type.getClass() );
		}

	}
}
