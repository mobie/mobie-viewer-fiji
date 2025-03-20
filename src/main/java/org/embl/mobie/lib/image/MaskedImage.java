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
package org.embl.mobie.lib.image;

import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.Type;
import org.embl.mobie.lib.source.mask.MaskedSource;
import org.embl.mobie.lib.source.mask.VolatileMaskedSource;
import org.embl.mobie.lib.util.MoBIEHelper;

public class MaskedImage< T extends Type< T > > implements Image< T >
{
	private final String name;
	private final Image< T > wrappedImage;
	private DefaultSourcePair< T > sourcePair;
	private RealMaskRealInterval mask;

	public MaskedImage( Image< T > wrappedImage, String name, RealMaskRealInterval mask )
	{
		this.wrappedImage = wrappedImage;
		this.name = name;
		this.mask = mask;
	}

	private synchronized void createSourcePair( )
	{
		if ( sourcePair != null ) return;

		SourcePair< T > inputSourcePair = MoBIEHelper.wrapTransformSourceAroundSourcePair( wrappedImage.getSourcePair() );
		final Source< T > source = inputSourcePair.getSource();
		final Source< ? extends Volatile< T > > volatileSource
				= inputSourcePair.getVolatileSource();

		MaskedSource< T > maskedSource = new MaskedSource<>( source, name, mask );
		VolatileMaskedSource< T, ? extends Volatile< T > > volatileMaskedSource
				= new VolatileMaskedSource<>( (Source) volatileSource, name, mask );

		sourcePair = new DefaultSourcePair<>( maskedSource, volatileMaskedSource );
	}


	@Override
	public SourcePair< T > getSourcePair()
	{
		if ( sourcePair == null )
			createSourcePair();

		return sourcePair;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		// FIXME transform  image
		throw new RuntimeException("Transforming a MaskedImage is not yet implemented.");
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}
}
