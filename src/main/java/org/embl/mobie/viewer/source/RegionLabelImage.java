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

import bdv.tools.transformation.TransformedSource;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import org.embl.mobie.viewer.annotation.ImageAnnotation;
import org.embl.mobie.viewer.color.AnnotationConverter;
import net.imglib2.Interval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;

public class RegionLabelImage< IA extends ImageAnnotation > implements Image< IntegerType >
{
	private final Set< IA > imageAnnotations;

	public RegionLabelImage( Set< IA > imageAnnotations )
	{
		this.imageAnnotations = imageAnnotations;
		//setUnionMask( regions );
		createImage();
	}

//	public void setUnionMask( List< T > tableRows )
//	{
//		size = tableRows.size();
//
//		for ( T tableRow : tableRows )
//		{
//			final RealMaskRealInterval mask = tableRow.mask();
//
//			if ( unionInterval == null )
//			{
//				//unionMask = mask;
//				unionInterval = mask;
//			}
//			else
//			{
//
//				if ( Intervals.equals(  mask, unionInterval ) )
//				{
//					continue;
//				}
//				else
//				{
//					// TODO: Below hangs
//					//unionMask = unionMask.or( mask );
//					unionInterval = Intervals.union( unionInterval, mask );
//				}
//			}
//		}
//
//		// TODO: this is a work around because the above hangs
//		unionMask = GeomMasks.closedBox( unionInterval.minAsDoubleArray(), unionInterval.maxAsDoubleArray() );
//	}

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

		final FunctionRealRandomAccessible< AnnotationType< T > > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, AnnotationType::new );
		final Interval interval = Intervals.smallestContainingInterval( unionMask );
		final RealRandomAccessibleIntervalSource source = new RealRandomAccessibleIntervalSource( randomAccessible, interval, new AnnotationType(), name );
	}

	private ArrayList< Integer > configureTimePoints()
	{
		// TODO: make this configurable in constructor or base it on the tableRows which have: tableRows.get( 0 ).timePoint()
		final ArrayList< Integer > timepoints = new ArrayList<>();
		timepoints.add( 0 );
		return timepoints;
	}

	@Override
	public SourcePair< IntegerType > getSourcePair()
	{
		return null;
	}

	public String getName()
	{
		return name;
	}
}
