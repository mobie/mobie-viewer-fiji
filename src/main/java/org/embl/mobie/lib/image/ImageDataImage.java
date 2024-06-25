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

import bdv.cache.SharedQueue;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.DataStore;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.source.SourceHelper;

import javax.annotation.Nullable;

/**
 * Converts various input resources into an {@code Image}.
 */
public class ImageDataImage< T extends NumericType< T > & NativeType< T > > implements Image< T >
{
	private ImageDataFormat imageDataFormat;
	private String uri;
	private int setupId;
	private SourcePair< T > sourcePair;
	private String name;
	private Site site;
	private SharedQueue sharedQueue;
	private VoxelDimensions voxelDimensions;
	@Nullable
	private RealMaskRealInterval mask;
	private TransformedSource< T > transformedSource;
	private AffineTransform3D currentTransform = new AffineTransform3D();

	public ImageDataImage( ImageData< ? > imageData, Integer setupId, String name, VoxelDimensions voxelDimensions )
	{
		this.imageDataFormat = null;
		this.uri = null;
		this.sharedQueue = null;
		this.setupId = setupId == null ? 0 : setupId;
		this.name = name; // FIXME: add the datasetname to the name ?!
		System.out.println("Name: " + name );
		System.out.println("SetupID: " + setupId );
		System.out.println("Dataset name: " + name );
		this.voxelDimensions = voxelDimensions;
		createSourcePair( ( ImageData< T > ) imageData, setupId, name );
	}

	public ImageDataImage(
			ImageDataFormat imageDataFormat,
			String uri,
			int setupId,
			String name,
			@Nullable SharedQueue sharedQueue,
			VoxelDimensions voxelDimensions )
	{
		this.imageDataFormat = imageDataFormat;
		this.uri = uri;
		this.setupId = setupId;
		this.name = name;
		this.sharedQueue = sharedQueue;
		this.voxelDimensions = voxelDimensions;
	}

	public ImageDataImage(
			Site site,
			String name,
			SharedQueue sharedQueue,
			VoxelDimensions voxelDimensions )
	{
		this.setupId = site.getChannel();
		this.name = name;
		this.site = site;
		this.sharedQueue = sharedQueue;
		this.voxelDimensions = voxelDimensions;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		if( sourcePair == null ) open();
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
		if ( mask != null )
		{
			// The mask contains potential previous transforms already,
			// thus we add the new transform on top.
			mask = mask.transform( affineTransform3D.inverse() );
		}

		if ( transformedSource != null )
		{
			transformedSource.getFixedTransform( currentTransform );
			currentTransform.preConcatenate( affineTransform3D );
			transformedSource.setFixedTransform( currentTransform );
		}
		else
		{
			// in case the image is transformed before it is instantiated
			currentTransform.preConcatenate( affineTransform3D );
		}

		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
	}

	@Override
	public RealMaskRealInterval getMask( )
	{
		if ( mask == null )
		{
			// It is important to include the voxel dimensions,
			// because otherwise rendering 2D sources in a 3D scene
			// will make them so thin that the {@code RegionLabelImage}
			// does not render anything.
			return SourceHelper.estimatePhysicalMask( getSourcePair().getSource(), 0, true );
		}

		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}

	private void open()
	{
		createSourcePair( openImageData(), setupId, name );
	}

	private void createSourcePair( ImageData< T > imageData, int setupId, String name )
	{
		final Source< T > source = imageData.getSourcePair( setupId ).getA();
		Source< ? extends Volatile< T > > volatileSource = imageData.getSourcePair( setupId ).getB();

		if ( voxelDimensions != null  )
		{
			source.getSourceTransform( 0, 0, currentTransform );
			// remove current spatial calibration
			currentTransform = currentTransform.inverse();
			// add new spatial calibration
			currentTransform.scale(
					voxelDimensions.dimension( 0 ),
					voxelDimensions.dimension( 1 ),
					voxelDimensions.dimension( 2 ) );
			SourceHelper.setVoxelDimensions( source, voxelDimensions );
			SourceHelper.setVoxelDimensions( volatileSource, voxelDimensions );
		}

		transformedSource = new TransformedSource<>( source, name );
		transformedSource.setFixedTransform( currentTransform );

		sourcePair = new DefaultSourcePair<>( transformedSource, new TransformedSource<>( volatileSource, transformedSource, name ) );
	}

	private ImageData< T > openImageData( )
	{
		if ( site != null )
		{
			return ( ImageData< T > ) DataStore.fetchImageData( site, sharedQueue );
		}

		return ( ImageData< T > ) DataStore.fetchImageData( uri, imageDataFormat, sharedQueue );
	}

	public String getUri()
	{
		return uri;
	}

	public int getSetupId()
	{
		return setupId;
	}
}
