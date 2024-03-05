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

import org.embl.mobie.lib.MoBIEHelper;

import java.util.*;

public class InterpolatedAffineTransformation extends AbstractImageTransformation
{
	// Serialization
	private TreeMap<Double, double[]> transforms;

	public InterpolatedAffineTransformation()
	{
	}

	public InterpolatedAffineTransformation( String name, TreeMap< Double, double[] > transforms, String sourceName, String transformedSourceName )
	{
		this.name = name;
		this.transforms = transforms;
		this.sources = sourceName == null ? null : Collections.singletonList( sourceName );
		this.sourceNamesAfterTransform = transformedSourceName == null ? null : Collections.singletonList( transformedSourceName );
	}

	public TreeMap< Double, double[] > getTransforms()
	{
		return transforms;
	}

	@Override
	public String toString()
	{
		List<String> lines = new ArrayList<>();

		lines.add("## Interpolated affine transformation: " + getName());

		transforms.forEach((z, affine) ->
				lines.add("Affine (z=" + MoBIEHelper.print(z, 3) + "): " + MoBIEHelper.print(affine, 3))
		);

		addSources( lines );

		return String.join("\n", lines);
	}

}
