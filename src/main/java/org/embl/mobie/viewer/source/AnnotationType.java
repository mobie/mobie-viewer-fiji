package org.embl.mobie.viewer.source;

import net.imglib2.type.Type;

public abstract class AnnotationType< T > implements Type< AnnotationType< T > >
{
	protected T annotation;

	public AnnotationType()
	{
	}

	public AnnotationType( T annotation )
	{
		this.annotation = annotation;
	}

	public T get()
	{
		return annotation;
	};
}
