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
package org.embl.mobie3.viewer.transform.image;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import org.embl.mobie3.viewer.source.DefaultImage;
import org.embl.mobie3.viewer.source.Image;
import org.embl.mobie3.viewer.source.SourcePair;
import org.embl.mobie3.viewer.transform.AbstractTransformation;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;
import java.util.List;

public class AffineTransformation extends AbstractTransformation
{
	// Serialisation
	protected double[] parameters;

	// Runtime
	private transient AffineTransform3D affineTransform3D = new AffineTransform3D();

	public AffineTransformation( String name, double[] parameters, List< String > sources ) {
		this( name, parameters, sources, null );
	}

	public AffineTransformation( String name, double[] parameters, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.parameters = parameters;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;

	}

	public AffineTransformation( TransformedSource< ? > transformedSource )
	{
		AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		name = "manualTransform";
		parameters = fixedTransform.getRowPackedCopy();
		sources	= Arrays.asList( transformedSource.getWrappedSource().getName() );
		sourceNamesAfterTransform =	Arrays.asList( transformedSource.getName() );
	}

	@Override
	public < T > Image< T > apply( Image< T > image )
	{
		affineTransform3D.set( parameters );

		final SourcePair< T > sourcePair = image.getSourcePair();
		final Source< T > source = sourcePair.getSource();
		final Source< ? extends Volatile< T > > volatileSource = sourcePair.getVolatileSource();
		final String transformedImageName = getTransformedName( image );

		final TransformedSource transformedSource = new TransformedSource( source, transformedImageName );
		transformedSource.setFixedTransform( affineTransform3D );
		final TransformedSource volatileTransformedSource = new TransformedSource( volatileSource, transformedSource );

		return new DefaultImage<>( transformedSource, volatileTransformedSource, name );
	}

	@Override
	public List< String > getTargetImages()
	{
		return sources;
	}

	public AffineTransform3D getAffineTransform3D()
	{
		return affineTransform3D;
	}
}
