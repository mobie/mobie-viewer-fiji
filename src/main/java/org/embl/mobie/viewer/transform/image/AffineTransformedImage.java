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
package org.embl.mobie.viewer.transform.image;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.image.DefaultSourcePair;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.source.SourcePair;

public class AffineTransformedImage< T > implements Image< T >
{
	private final AffineTransform3D affineTransform3D;
	private final Image< T > image;
	private final String transformedImageName;
	private DefaultSourcePair sourcePair;

	public AffineTransformedImage( Image< T > image, String transformedImageName, AffineTransform3D affineTransform3D )
	{
		this.image = image;
		this.transformedImageName = transformedImageName;
		this.affineTransform3D = affineTransform3D;
	}

	private void createSourcePair()
	{
		if ( sourcePair != null) return;

		final SourcePair< T > sourcePair = image.getSourcePair();
		final Source< T > source = sourcePair.getSource();
		final Source< ? extends Volatile< T > > volatileSource = sourcePair.getVolatileSource();

		final TransformedSource transformedSource = new TransformedSource( source, transformedImageName );
		transformedSource.setFixedTransform( affineTransform3D );
		final TransformedSource volatileTransformedSource = new TransformedSource( volatileSource, transformedSource );
		this.sourcePair = new DefaultSourcePair<>( transformedSource, volatileTransformedSource );
	}

	public AffineTransform3D getAffineTransform3D()
	{
		return affineTransform3D;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		createSourcePair();
		return sourcePair;
	}

	@Override
	public String getName()
	{
		return transformedImageName;
	}

	@Override
	public RealMaskRealInterval getMask( )
	{
		final RealMaskRealInterval mask = image.getMask();
//		final double[] min = mask.minAsDoubleArray();
//		final double[] max = mask.maxAsDoubleArray();
		final RealMaskRealInterval realMaskRealInterval = mask.transform( affineTransform3D.inverse() );
//		final double[] min1 = realMaskRealInterval.minAsDoubleArray();
//		final double[] max1 = realMaskRealInterval.maxAsDoubleArray();
		return realMaskRealInterval;
	}
}
