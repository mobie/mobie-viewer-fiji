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
package org.embl.mobie.lib.image;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.roi.RealMaskRealInterval;
import org.checkerframework.checker.units.qual.A;
import org.embl.mobie.lib.source.RealTransformedSource;

public class RealTransformedImage< T > implements Image< T >
{
	protected final RealTransform realTransform;
	protected final Image< T > image;
	protected final String name;
	private RealMaskRealInterval mask;
	private final AffineTransform3D affineTransform3D = new AffineTransform3D();

	public RealTransformedImage( Image< T > image, String name, RealTransform realTransform )
	{
		this.image = image;
		this.name = name;
		this.realTransform = realTransform;
	}

	public RealTransform getRealTransform()
	{
		return realTransform;
	}

	@Override
	public synchronized SourcePair< T > getSourcePair()
	{
		final SourcePair< T > sourcePair = image.getSourcePair();

		final Source< T > source = sourcePair.getSource();
		final Source< ? extends Volatile< T > > volatileSource = sourcePair.getVolatileSource();

		RealTransformedSource< T > realTransformedSource = new RealTransformedSource<>( source, realTransform, name );
		RealTransformedSource< ? extends Volatile< T > > realTransformedVolatileSource = new RealTransformedSource<>( volatileSource, realTransform, name );

		final TransformedSource< T > transformedSource = new TransformedSource<>( realTransformedSource, name );
		final TransformedSource< ? extends Volatile< T > > volatileTransformedSource = new TransformedSource<>( realTransformedVolatileSource, transformedSource );
		transformedSource.setFixedTransform( affineTransform3D );

		return new DefaultSourcePair<>( transformedSource, volatileTransformedSource );
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		this.affineTransform3D.preConcatenate( affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		if ( mask == null )
		{
			// FIXME: this should be something like
			//   image.getMask().transform( realTransform.inverse() )
			//   and probably also include this.affineTransform
			System.err.println( "Masks for " + this.getClass().getName() + " are not properly implemented" );
			return image.getMask();
		}
		else
			return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}
}
