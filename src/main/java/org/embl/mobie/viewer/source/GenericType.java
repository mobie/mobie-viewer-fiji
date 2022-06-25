package org.embl.mobie.viewer.source;

import net.imglib2.type.Type;

public abstract class GenericType< T > implements Type< GenericType< T > >
{
	protected T object;

	public GenericType()
	{
	}

	public GenericType( T object )
	{
		this.object = object;
	}

	public T get()
	{
		return object;
	};
}
