package de.embl.cba.mobie.transform;

import net.imglib2.type.numeric.NumericType;

public abstract class AbstractSourceTransformer< T extends NumericType< T > > implements SourceTransformer< T >
{
	// Serialisation
	protected String name;
}
