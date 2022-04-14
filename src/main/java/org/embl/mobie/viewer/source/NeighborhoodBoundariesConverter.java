/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * This version of NeighborhoodNonZeroBoundariesConverter
 * might be faster, but one does need to know the RAI
 * on which to compute on upon time of construction.
 *
 * @param <R>
 */
public class NeighborhoodBoundariesConverter< R extends RealType< R > >
		implements Converter< Neighborhood< R >, R >
{
	private final RandomAccessibleInterval< R > rai;
	private final RandomAccess< R > randomAccess;
	private final float background;

	public NeighborhoodBoundariesConverter( RandomAccessibleInterval< R > rai, float background )
	{
		this.rai = rai;
		this.randomAccess = rai.randomAccess();
		this.background = background;
	}

	@Override
	public void convert( Neighborhood< R > neighborhood, R output )
	{
		final R centerValue = getCenterValue( neighborhood );

		if ( output instanceof Volatile )
		{
			if ( !( ( Volatile ) centerValue ).isValid() )
			{
				( ( Volatile< ? > ) output ).setValid( false );
				return;
			}
		}

		final float centerFloat = centerValue.getRealFloat();

		output.setReal( background );

		if ( centerFloat == background ) return;

		for ( R value : neighborhood )
		{
			if ( value.getRealFloat() != centerFloat )
			{
				output.setReal( centerFloat );
				return;
			}
		}
	}

	private R getCenterValue( Neighborhood< R > neighborhood )
	{
		long[] centrePosition = new long[ neighborhood.numDimensions() ];
		neighborhood.localize( centrePosition );
		return randomAccess.setPositionAndGet( centrePosition );
	}

	public static < R extends RealType< R > >
	RandomAccessibleInterval< R > getNeighborhoodConvertedView(
			RandomAccessibleInterval< R > rai,
			Converter< Neighborhood< R >, R > neighborhoodConverter,
			Shape shape,
			float background )
	{
		final RandomAccessible< Neighborhood< R > > nra =
				shape.neighborhoodsRandomAccessible(
						Views.extendValue( rai, background ) );

		final RandomAccessibleInterval< Neighborhood< R > > nrai
				= Views.interval( nra, rai );

		return Converters.convert( nrai,
				neighborhoodConverter,
				Util.getTypeFromInterval( rai ) );
	}
}
