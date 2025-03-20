/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.source.label;

import net.imglib2.Volatile;
import net.imglib2.type.Type;
import org.embl.mobie.lib.source.AnnotationType;

// Note: This must be a type (as in right now implementing Type< VolatileAnnotationType< T > > ) otherwise it cannot be a pixel in a Source
public class VolatileAnnotationType< T > extends Volatile< AnnotationType< T > > implements Type< VolatileAnnotationType< T > >
{
	public VolatileAnnotationType( )
	{
		super( new AnnotationType<>( null ), false );
	}

	public VolatileAnnotationType( T annotation, boolean valid )
	{
		super( new AnnotationType<>( annotation ), valid );
	}

	@Override
	public VolatileAnnotationType< T > createVariable()
	{
		return new VolatileAnnotationType<>( null, true );
	}

	@Override
	public VolatileAnnotationType< T > copy()
	{
		final VolatileAnnotationType< T > volatileAnnotationType = createVariable();
		volatileAnnotationType.set( this );
		return volatileAnnotationType;
	}

	@Override
	public void set( VolatileAnnotationType< T > c )
	{
		valid = c.isValid();
		t.set( c.get() );
	}

	@Override
	public boolean valueEquals( VolatileAnnotationType< T > va )
	{
		return t.valueEquals( va.t );
	}

	public T getAnnotation()
	{
		return get().getAnnotation();
	}
}
