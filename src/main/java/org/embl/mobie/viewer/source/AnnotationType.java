package org.embl.mobie.viewer.source;

import net.imglib2.type.Type;

public class AnnotationType< T > implements Type< AnnotationType< T > >
{
	protected T annotation;

	// TODO: remove this constructor once
	//   we have an implementation for "background annotation"
	//   currently null means this is background (no annotation)
	public AnnotationType()
	{
	}

	public AnnotationType( T annotation )
	{
		// TODO: assert that this is not null (s.a.)
		this.annotation = annotation;
	}

	public T getAnnotation()
	{
		return annotation;
	};

	@Override
	public AnnotationType< T > createVariable()
	{
		return new AnnotationType<>();
	}

	@Override
	public AnnotationType< T > copy()
	{
		final AnnotationType< T > annotationType = createVariable();
		annotationType.set( this );
		return annotationType;
	}

	@Override
	public void set( AnnotationType< T > c )
	{
		annotation = c.annotation;
	}

	@Override
	public boolean valueEquals( AnnotationType< T > annotationType )
	{
		/*
		currently null is the background annotation
		thus null == null should return true
		TODO: probably it is safer to implement a dedicate
		 background object for each "annotation" (s.a.).
		 */
		if ( annotation == null )
		{
			if ( annotationType.getAnnotation() == null )
				return true;
			else
				return false;
		}
		else
		{
			return annotation.equals( annotationType.getAnnotation() );
		}
	}
}
