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
package org.embl.mobie.lib.serialize.transformation;


import java.util.Arrays;
import java.util.List;

public abstract class AbstractImageTransformation implements ImageTransformation
{
	// Serialisation
	protected String name;
	protected String description; // FIXME: Add to spec

	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	@Override
	public String getTransformedImageName( String imageName )
	{
		if ( sourceNamesAfterTransform == null )
			return null;

		return sourceNamesAfterTransform.get( sources.indexOf( imageName ) );
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	protected void addDescription( List< String > lines )
	{
		if(name != null)
			lines.add("Name: " + name);

		if(description != null)
			lines.add("Description: " + name);
	}

	protected void addSources( List< String > lines )
	{
		if ( sources != null )
			lines.add("Input source(s): " + Arrays.toString(sources.toArray()));

		if ( sourceNamesAfterTransform != null )
			lines.add("Output source(s): " + Arrays.toString(sourceNamesAfterTransform.toArray()));
		else if ( sources != null )
			lines.add("Output source(s): " + Arrays.toString(sources.toArray()));
	}
}
