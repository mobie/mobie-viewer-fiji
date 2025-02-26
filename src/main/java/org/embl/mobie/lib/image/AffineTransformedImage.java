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

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.util.MoBIEHelper;

public class AffineTransformedImage< T > implements Image< T >, TransformedImage
{
	private final Image< T > image;
	private final String name;
	private final AffineTransform3D affineTransform3D;

	private Transformation transformation;

	private RealMaskRealInterval mask;
	private DefaultSourcePair< T > sourcePair;

	public AffineTransformedImage(
			Image< T > image,
			String transformedImageName,
			AffineTransformation transformation )
	{
		this.image = image;
		this.name = transformedImageName;
		this.transformation = transformation;
		this.affineTransform3D = transformation.getAffineTransform3D();
	}

	public Transformation getTransformation()
	{
		return transformation;
	}

	@Override
	public void setTransformation( Transformation transformation )
	{
		this.transformation = transformation;
	}

	@Override
	public synchronized SourcePair< T > getSourcePair()
	{
		if ( sourcePair == null )
			createSourcePair();

		return sourcePair;
	}

	private void createSourcePair()
	{
		// TODO: Is it really necessary to "wrapTransformSourceAroundSourcePair" here?
		//       This is happening here anyway...
		SourcePair< T > defaultSourcePair = MoBIEHelper.wrapTransformSourceAroundSourcePair( image.getSourcePair() );

		final TransformedSource< T > transformedSource = new TransformedSource<>( defaultSourcePair.getSource(), name );
		transformedSource.setFixedTransform( affineTransform3D );

		if ( defaultSourcePair.getVolatileSource() == null )
		{
			this.sourcePair = new DefaultSourcePair<>( transformedSource, null );
			return;
		}

		final TransformedSource< ? extends Volatile< T > > volatileTransformedSource
				= new TransformedSource<>( defaultSourcePair.getVolatileSource(), transformedSource, name );
		this.sourcePair = new DefaultSourcePair<>( transformedSource, volatileTransformedSource );
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
	public RealMaskRealInterval getMask( )
	{
		if ( mask == null )
			return image.getMask().transform( affineTransform3D.inverse() );
		else
			return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}

	@Override
	public Image< ? > getWrappedImage()
	{
		return image;
	}
}
