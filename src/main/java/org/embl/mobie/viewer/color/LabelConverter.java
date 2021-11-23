/*-
 * #%L
 * TODO
 * %%
 * Copyright (C) 2018 - 2020 EMBL
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
package org.embl.mobie.viewer.color;

import bdv.viewer.TimePointListener;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.volatiles.VolatileUnsignedIntType;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.segment.SegmentAdapter;
import de.embl.cba.tables.imagesegment.ImageSegment;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class LabelConverter< S extends ImageSegment > implements Converter< RealType, ARGBType >, TimePointListener, OpacityAdjuster
{
	private final SegmentAdapter< S > segmentAdapter;
	private final String imageId;
	private final MoBIEColoringModel< S > coloringModel;

	private int timePointIndex = 0;
	private double opacity = 1.0;

	// No imageId given => decode from pixel value
	public LabelConverter(
			SegmentAdapter< S > segmentAdapter,
			MoBIEColoringModel< S > coloringModel )
	{
		this.segmentAdapter = segmentAdapter;
		this.imageId = null;
		this.coloringModel = coloringModel;
	}

	public LabelConverter(
			SegmentAdapter< S > segmentAdapter,
			String imageId,
			MoBIEColoringModel< S > coloringModel )
	{
		this.segmentAdapter = segmentAdapter;
		this.imageId = imageId;
		this.coloringModel = coloringModel;
	}

	@Override
	public void convert( RealType label, ARGBType color )
	{
		if ( label instanceof Volatile )
		{
			if ( !( ( Volatile ) label ).isValid() )
			{
				color.set( 0 );
				return;
			}
		}

		if ( label.getRealDouble() == 0 )
		{
			color.set( 0 );
			return;
		}

		S imageSegment = getImageSegment( label );

		if ( imageSegment == null )
		{
			color.set( 0 );
			return;
		}
		else
		{
			coloringModel.convert( imageSegment, color );
			final int alpha = ARGBType.alpha( color.get() );
			color.mul( alpha / 255.0 );
		}

		color.mul( opacity );
	}

	// TODO: figure out how to make this work for more types
	private S getImageSegment( RealType label )
	{
		if ( imageId == null )
		{
			final long value = SourceNameEncoder.getValue( ( VolatileUnsignedIntType ) label );
			if ( value == 0 )
			{
				return null; // background
			}
			final String imageId = SourceNameEncoder.getName( ( VolatileUnsignedIntType ) label );
			return segmentAdapter.getSegmentCreateIfNotExist( value, timePointIndex, imageId );
		}
		else
		{
			return segmentAdapter.getSegmentCreateIfNotExist( label.getRealDouble(), timePointIndex, imageId );
		}
	}

	@Override
	public void timePointChanged( int timePointIndex )
	{
		this.timePointIndex = timePointIndex;
	}

	@Override
	public void setOpacity( double opacity )
	{
		this.opacity = opacity;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	public MoBIEColoringModel< S > getColoringModel()
	{
		return coloringModel;
	}
}
