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

import org.embl.mobie.io.util.IOHelper;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ThinPlateSplineTransformation extends AbstractImageTransformation
{
    // Serialisation
	protected String landmarksJson;

	public ThinPlateSplineTransformation( String name, String parameters, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;

		// parameters may already contain JSON content or be a URI/path to a JSON file.
		this.landmarksJson = resolveParametersToJson( parameters );
	}

	private boolean isJSON( String string )
	{
		String trim = string.trim();
		return trim.startsWith( "{" ) || trim.startsWith( "[" );
	}

	private String resolveParametersToJson( String parameters )
	{
		if ( parameters == null ) return null;

		if ( isJSON( parameters ) )
		{
			return parameters;
		}

		// Otherwise open the file as a JSON
        try
        {
			String read = IOHelper.read( parameters );

			if ( isJSON( read ) )
	        	return read;
			else
				throw new RuntimeException( "Thin plate spline must be in JSON format, but this file is not: " + parameters );
        }
		catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

	public String getLandmarksJson()
	{
		return landmarksJson;
	}

	@Override
	public String toString()
	{
		List<String> lines = new ArrayList<>();

		lines.add( "Thin plate spline transformation: " + getName() );

		lines.add( landmarksJson );

		addSources( lines );

		return String.join( "\n", lines );
	}
}
