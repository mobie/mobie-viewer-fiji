package org.embl.mobie.viewer.source;

import net.imglib2.type.Type;

public class AnnotationType< T > implements Type< AnnotationType< T > >
{
	protected T annotation;

	public AnnotationType()
	{
		this.annotation = null; // == background
	}

	public AnnotationType( T annotation )
	{
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
		TODO: maybe better to implement a dedicate
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

	public void setAnnotation( T annotation )
	{
		this.annotation = annotation;
	}
}
