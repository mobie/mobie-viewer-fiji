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

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.MoBIEHelper;

import java.util.ArrayList;
import java.util.List;

public class AffineTransformation extends AbstractImageTransformation
{
	// Serialisation
	protected double[] parameters;

	public AffineTransformation( String name, double[] parameters, List< String > sources ) {
		this( name, parameters, sources, null );
	}

	public AffineTransformation( String name, AffineTransform3D affineTransform3D, List< String > sources ) {
		this( name, affineTransform3D.getRowPackedCopy(), sources, null );
	}

	public AffineTransformation( String name, double[] parameters, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.parameters = parameters;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}


	public AffineTransform3D getAffineTransform3D()
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );
		return affineTransform3D;
	}

	@Override
	public String toString()
	{
		List<String> lines = new ArrayList<>();

		lines.add( "## Affine transformation: " + getName() );

		String affineString = MoBIEHelper.print( parameters, 3 );

		// split into three rows for better readability
		String[] entries = affineString.split(",");
		StringBuilder output = new StringBuilder();
		int count = 0;
        for ( String entry : entries )
        {
            output.append( entry );
            count++;
            if ( count == 4 )
            {
                lines.add( output.toString() );
                count = 0;
            }
			else
            {
                output.append( "," );
            }
        }

		addSources( lines );

		return String.join( "\n", lines );
	}
}
