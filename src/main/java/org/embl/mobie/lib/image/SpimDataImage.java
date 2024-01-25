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

import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.cache.SharedQueue;
import bdv.tools.transformation.TransformedSource;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.source.SourceHelper;

import javax.annotation.Nullable;

public class SpimDataImage< T extends NumericType< T > & RealType< T > > implements Image< T >
{
	private ImageDataFormat imageDataFormat;
	private String path;
	private int setupId = 0;
	private SourcePair< T > sourcePair;
	private String name;
	private Site site;
	private SharedQueue sharedQueue;
	private Boolean removeSpatialCalibration = false;
	@Nullable
	private RealMaskRealInterval mask;
	private TransformedSource< T > transformedSource;
	private AffineTransform3D currentTransform = new AffineTransform3D();

	public SpimDataImage( AbstractSpimData< ? > spimData, Integer setupId, String name, Boolean removeSpatialCalibration  )
	{
		this.imageDataFormat = null;
		this.path = null;
		this.sharedQueue = null;
		this.setupId = setupId == null ? 0 : setupId;
		this.name = name;
		this.removeSpatialCalibration = removeSpatialCalibration;
		createSourcePair( spimData, setupId, name );
	}

	public SpimDataImage( ImageDataFormat imageDataFormat, String path, int setupId, String name, @Nullable SharedQueue sharedQueue, Boolean removeSpatialCalibration )
	{
		this.imageDataFormat = imageDataFormat;
		this.path = path;
		this.setupId = setupId;
		this.name = name;
		this.sharedQueue = sharedQueue;
		this.removeSpatialCalibration = removeSpatialCalibration;
	}

	public SpimDataImage( Site site, String name, SharedQueue sharedQueue, Boolean removeSpatialCalibration )
	{
		this.setupId = site.getChannel();
		this.name = name;
		this.site = site;
		this.sharedQueue = sharedQueue;
		this.removeSpatialCalibration = removeSpatialCalibration;
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
			return SourceHelper.estimateMask( getSourcePair().getSource(), 0, true );
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
		final AbstractSpimData< ? > spimData = openSpimData();

		createSourcePair( spimData, setupId, name );
	}

	private void createSourcePair( AbstractSpimData< ? > spimData, int setupId, String name )
	{
		final SpimSource< T > source = new SpimSource<>( spimData, setupId, name );
		final VolatileSpimSource< ? extends Volatile< T > > vSource = new VolatileSpimSource<>( spimData, setupId, name );

		if ( removeSpatialCalibration )
		{
			source.getSourceTransform( 0, 0, currentTransform );
			currentTransform = currentTransform.inverse();
			SourceHelper.setVoxelDimensionsToPixels( source );
			SourceHelper.setVoxelDimensionsToPixels( vSource );
		}

		transformedSource = new TransformedSource<>( source );
		transformedSource.setFixedTransform( currentTransform );

		sourcePair = new DefaultSourcePair<>( transformedSource, new TransformedSource<>( vSource, transformedSource ) );
	}

	private AbstractSpimData openSpimData( )
	{
		if ( site != null )
		{
			return DataStore.fetchSpimData( site, sharedQueue );
		}

		return DataStore.fetchSpimData( path, imageDataFormat, sharedQueue );
	}

}
