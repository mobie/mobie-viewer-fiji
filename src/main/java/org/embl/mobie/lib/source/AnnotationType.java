/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.source;

import net.imglib2.type.Type;

public class AnnotationType< A > implements Type< AnnotationType< A > >
{
	protected A annotation;

	public AnnotationType()
	{
		this.annotation = null; // == background
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
		/*
		currently null is the background annotation
		thus null == null should return true
		TODO: maybe better to implement a dedicate
		 background object for each "annotation" (s.a.).
		 */
		if ( annotation == null )
		{
			if ( annotationType.getAnnotation() == null )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return annotation.equals( annotationType.getAnnotation() );
		}
	}

	public void setAnnotation( A annotation )
	{
		this.annotation = annotation;
	}
}
