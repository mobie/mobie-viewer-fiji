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

import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.serialize.transformation.ThinPlateSplineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;

public class TpsTransformedImage< T > implements Image< T >, TransformedImage
{
	private final Image< T > image;
	private final String name;
	private final String landmarksJson;
	private Transformation transformation;
	private final AffineTransform3D affineTransform3D = new AffineTransform3D();

	private RealMaskRealInterval mask;
	private DefaultSourcePair< T > sourcePair;

	public TpsTransformedImage(
			Image< T > image,
			String transformedImageName,
			ThinPlateSplineTransformation transformation )
	{
		this.image = image;
		this.name = transformedImageName;
		this.transformation = transformation;
		this.landmarksJson = transformation.getLandmarksJson();
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
		// Create the transformation
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		JsonElement json = JsonParser.parseString( landmarksJson );
		ltm.fromJson( json );
		BigWarpTransform bigWarpTransform = new BigWarpTransform( ltm, BigWarpTransform.TPS );
		InvertibleRealTransform invertibleRealTransform = bigWarpTransform.getTransformation();

		// Apply the transformation
		SourcePair< T > originalSourcePair = image.getSourcePair();

		WarpedSource< T > warpedSource = new WarpedSource<>( originalSourcePair.getSource(), "" );
		warpedSource.updateTransform( invertibleRealTransform );
		warpedSource.setIsTransformed( true );

		WarpedSource< ? extends Volatile< T > > warpedVolatileSource = new WarpedSource<>( originalSourcePair.getVolatileSource(), "" );
		warpedVolatileSource.updateTransform( invertibleRealTransform );
		warpedVolatileSource.setIsTransformed( true );

		// Wrap into a transformed source such that they have a shared affine transform
		final TransformedSource< T > transformedSource = new TransformedSource<>( warpedSource, name );
		final TransformedSource< ? extends Volatile< T > > volatileTransformedSource = new TransformedSource<>( warpedVolatileSource, transformedSource );
		transformedSource.setFixedTransform( affineTransform3D );

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
