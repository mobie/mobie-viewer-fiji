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
package org.embl.mobie.lib.serialize.transformation;

import java.util.ArrayList;
import java.util.List;

public class ElastixBSplineTransformation extends AbstractImageTransformation
{
    // Serialisation
	protected String transformParametersFile;
	protected boolean invert;

	public ElastixBSplineTransformation(
			String name,
			String transformParametersFile,
			List< String > sources,
			List< String > sourceNamesAfterTransform,
			boolean invert )
	{
        this.name = name;
		this.transformParametersFile = transformParametersFile;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
		this.invert = invert;
	}

	public ElastixBSplineTransformation(
			String name,
			String transformParametersFile,
			List< String > sources,
			List< String > sourceNamesAfterTransform )
	{
        this.name = name;
		this.transformParametersFile = transformParametersFile;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}

	public String getTransformParametersUri()
	{
		return transformParametersFile;
	}

	public boolean isInvert()
	{
		return invert;
	}

	@Override
	public String toString()
	{
		List< String > lines = new ArrayList<>();

		lines.add( "Elastix BSpline transformation: " + getName() );
		lines.add( "Invert: " + invert );
		// Physical units are assumed to match the source units.
		lines.add( transformParametersFile );
		addSources( lines );

		return String.join( "\n", lines );
	}
}

