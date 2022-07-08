/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package mobie3.viewer.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import mobie3.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AffineSourceTransformer extends AbstractSourceTransformer
{
	// Serialisation
	protected double[] parameters;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	// Runtime
	private transient AffineTransform3D affineTransform3D;

	public AffineSourceTransformer( String name, double[] parameters, List< String > sources ) {
		this( name, parameters, sources, null );
	}

	public AffineSourceTransformer( String name, double[] parameters, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.parameters = parameters;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}

	public AffineSourceTransformer( TransformedSource< ? > transformedSource )
	{
		AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		name = "manualTransform";
		parameters = fixedTransform.getRowPackedCopy();
		sources	= Arrays.asList( transformedSource.getWrappedSource().getName() );
		sourceNamesAfterTransform =	Arrays.asList( transformedSource.getName() );
	}

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );

		for ( String sourceName : sourceNameToSourceAndConverter.keySet() )
		{
			if ( sources.contains( sourceName ) )
			{
				SourceAffineTransformer transformer = createSourceAffineTransformer( sourceName );

				final SourceAndConverter transformedSource = transformer.apply( sourceNameToSourceAndConverter.get( sourceName ) );

				sourceNameToSourceAndConverter.put( transformedSource.getSpimSource().getName(), transformedSource );
			}
		}
	}

	private SourceAffineTransformer createSourceAffineTransformer( String sourceName )
	{
		if ( sourceNamesAfterTransform != null )
		{
			return new SourceAffineTransformer( affineTransform3D, sourceNamesAfterTransform.get( sources.indexOf( sourceName ) ), false );
		}
		else
		{
			return new SourceAffineTransformer( affineTransform3D, sourceName, false );
		}
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}
}
