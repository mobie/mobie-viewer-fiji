package org.embl.mobie.viewer.source;

import net.imglib2.type.Type;

public class AnnotationType< A > implements Type< AnnotationType< A > >
{
	protected A annotation;

	public AnnotationType()
	{
	}

	public AnnotationType( A annotation )
	{
		this.annotation = annotation;
	}

	public A getAnnotation()
	{
		return annotation;
	};

	@Override
	public AnnotationType< A > createVariable()
	{
		return new AnnotationType<>();
	}

	@Override
	public AnnotationType< A > copy()
	{
		final AnnotationType< A > annotationType = createVariable();
		annotationType.set( this );
		return annotationType;
	}

	@Override
	public void set( AnnotationType< A > c )
	{
		annotation = c.annotation;
	}

	@Override
	public boolean valueEquals( AnnotationType< A > annotationType )
	{
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
