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
package org.embl.mobie.lib.table.saw;

import net.imglib2.realtransform.*;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.ImageListener;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.transform.TransformHelper;

import java.util.List;
import java.util.Set;

public class TableSawAnnotatedRegion extends AbstractTableSawAnnotation implements AnnotatedRegion, ImageListener
{
	private static final String[] idColumns = new String[]{ ColumnNames.REGION_ID };

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

	public TableSawAnnotatedRegion(
			final TableSawAnnotationTableModel< TableSawAnnotatedRegion > model,
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
			final RealMaskRealInterval unionMask = TransformHelper.createUnionMask( images );

			if ( relativeDilation > 0 )
			{
				mask = unionMask;
				double scale = 1 + relativeDilation;
				int numDimensions = mask.numDimensions();
				if ( numDimensions == 2 )
				{
					AffineTransform2D transform2D = new AffineTransform2D();
					transform2D.scale( scale );
					mask = mask.transform( transform2D );
				}
				else if ( numDimensions == 3 )
				{
					AffineTransform3D transform3D = new AffineTransform3D();
					transform3D.scale( scale );
					mask = mask.transform( transform3D );
				}

				int a = 1;
				// FIXME: This does not work with rotated masks!
				//   ask if one can dilate a mask in Zulip:

//				final double[] min = unionMask.minAsDoubleArray();
//				final double[] max = unionMask.maxAsDoubleArray();
//
//				for ( int d = 0; d < min.length; d++ )
//				{
//					final double size = max[ d ] - min[ d ];
//					min[ d ] -= size * relativeDilation;
//					max[ d ] += size * relativeDilation;
//				}
//
//				mask = GeomMasks.closedBox( min, max );
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

	public List< String > getRegionImageNames()
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
