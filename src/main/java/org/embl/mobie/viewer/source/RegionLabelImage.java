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
package org.embl.mobie.viewer.source;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.annotation.ImageAnnotation;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;

public class RegionLabelImage< IA extends ImageAnnotation > implements Image< UnsignedIntType >
{
	private final String name;
	private final Set< IA > imageAnnotations;
	private RealInterval realInterval;
	private Source< UnsignedIntType > source;
	private Source< ? extends Volatile< UnsignedIntType > > volatileSource = null;

	public RegionLabelImage( String name, Set< IA > imageAnnotations )
	{
		this.name = name;
		this.imageAnnotations = imageAnnotations;
		setImageMask();
		createImage();
	}

	private void setImageMask()
	{
		for ( IA imageAnnotation : imageAnnotations )
		{
			final RealMaskRealInterval mask = imageAnnotation.mask();

			if ( realInterval == null )
			{
				realInterval = mask;
			}
			else
			{
				if ( Intervals.equals(  mask, realInterval ) )
				{
					continue;
				}
				else
				{
					// TODO: Below hangs (see issue in imglib2-roi)
					//unionMask = unionMask.or( mask );
					realInterval = Intervals.union( realInterval, mask );
				}
			}
		}
	}

	private void createImage()
	{
		BiConsumer< RealLocalizable, IntegerType > biConsumer = ( location, value ) ->
		{
			for ( IA imageAnnotation : imageAnnotations )
			{
				if ( imageAnnotation.mask().test( location ) )
				{
					value.setInteger( imageAnnotation.label() );
					return;
				}
			}
			value.set( new UnsignedIntType() );
		};

		final ArrayList< Integer > timePoints = configureTimePoints();
		final Interval interval = Intervals.smallestContainingInterval( realInterval );
		final FunctionRealRandomAccessible< UnsignedIntType > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, UnsignedIntType::new );
		source = new RealRandomAccessibleIntervalTimelapseSource<>( randomAccessible, interval, new UnsignedIntType(), new AffineTransform3D(), name, false, timePoints );

		// TODO create volatile source
	}

	private ArrayList< Integer > configureTimePoints()
	{
		final ArrayList< Integer > timePoints = new ArrayList<>();
		timePoints.add( 0 );
		return timePoints;
	}

	@Override
	public SourcePair< UnsignedIntType > getSourcePair()
	{
		return new DefaultSourcePair<>( source, volatileSource );
	}

	public String getName()
	{
		return name;
	}
}
