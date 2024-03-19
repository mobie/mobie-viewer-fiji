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
package org.embl.mobie.lib.table.saw;

import net.imglib2.realtransform.*;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.ImageListener;
import org.embl.mobie.lib.transform.TransformHelper;

import java.util.List;
import java.util.Set;

public class TableSawAnnotatedImages extends AbstractTableSawAnnotation implements AnnotatedRegion, ImageListener
{
	private final List< String > imageNames;
	private final String uuid;
	private String regionId;
	private final int labelId;
	private final Integer timePoint;
	private double[] position;
	private String source;
	private RealMaskRealInterval mask;
	private Set< Image< ? > > images;
	private final double relativeDilation;

	public TableSawAnnotatedImages(
			final TableSawAnnotationTableModel< TableSawAnnotatedImages > model,
			final int rowIndex,
			final List< String > imageNames,
			final Integer timePoint,
			final String regionId,
			final int labelId,
			final String uuid,
			final double relativeDilation)
	{
		super( model, rowIndex );
		this.source = model.getDataSourceName();
		this.imageNames = imageNames;
		this.regionId = regionId;
		this.timePoint = timePoint;
		this.labelId = labelId;
		this.uuid = uuid;
		this.relativeDilation = relativeDilation;

		images = DataStore.getImageSet( imageNames );

		for ( Image< ? > image : images )
			image.listeners().add( this );
	}

	@Override
	public int label()
	{
		return labelId;
	}

	@Override
	public Integer timePoint()
	{
		return timePoint;
	}

	@Override
	public synchronized double[] positionAsDoubleArray()
	{
		if ( position == null )
		{
			final RealMaskRealInterval mask = getMask();
			final double[] min = Intervals.minAsDoubleArray( mask );
			final double[] max = Intervals.maxAsDoubleArray( mask );
			position = new double[ min.length ];
			for ( int d = 0; d < min.length; d++ )
				position[ d ] = ( max[ d ] + min[ d ] ) / 2.0;
		}

		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
	}

	@Override
	public String uuid()
	{
		return uuid;
	}

	@Override
	public String source()
	{
		return source;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		// don't do anything here, because the annotated regions
		// provide all the spatial coordinates.
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		if ( mask == null )
		{
			// Compute the mask of the images
			// that are annotated by this region
			RealMaskRealInterval unionMask;
			if ( images.size() > 1 )
			{
				//unionMask = TransformHelper.unionBox( images );
				unionMask = TransformHelper.union( images );
			}
			else
			{
				unionMask = images.iterator().next().getMask();
			}

			if ( relativeDilation > 0 )
			{
				double scale = 1 + relativeDilation;
				AffineGet transform = TransformHelper.getEnlargementTransform( unionMask, scale );
				mask = unionMask.transform( transform );
			}
			else
			{
				mask = unionMask;
			}
		}

		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		throw new RuntimeException();
	}

	@Override
	public String regionId()
	{
		return regionId;
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}

	public List< String > getImageNames()
	{
		return imageNames;
	}

	@Override
	public void imageChanged()
	{
		//System.out.println("Image changed: " + imageNames.get( 0 ) );
		mask = null; // force to compute the mask again
		position = null; // force to compute the position again
	}
}
