/*-
 * #%L
 * Various Java code for ImageJ
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
package org.embl.mobie.lib.plot;

import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.Random;

// For testing
public class RandomSpotsCreator< T extends NumericType< T > >
{
	private final T fixedValue;
	private int n;
	private RealInterval interval;
	private ArrayList< RealPoint > randomPointList;
	private ArrayList< T > fixedValueList;

	public RandomSpotsCreator( T fixedValue, int n, RealInterval interval )
	{
		this.fixedValue = fixedValue;
		this.n = n;
		this.interval = interval;
		createRandomPoints();
	}

	private void createRandomPoints()
	{
		Random rand = new Random( 60 );

		int nd = interval.numDimensions();
		double[] widths = new double[ nd ];
		double[] offset = new double[ nd ];
		for ( int d = 0; d < nd; d++ )
		{
			offset[ d ] = interval.realMin( d );
			widths[ d ] = interval.realMax( d ) - interval.realMin( d );
		}

		randomPointList = new ArrayList<>();
		fixedValueList = new ArrayList<>();

		for ( int i = 0; i < n; i++ )
		{
			RealPoint p = new RealPoint( nd );
			for ( int d = 0; d < nd; d++ )
				p.setPosition( offset[ d ] + rand.nextDouble() * widths[ d ], d );

			randomPointList.add( p );
			fixedValueList.add( fixedValue.copy() );
		}
	}

	public ArrayList< RealPoint > getPointList()
	{
		return randomPointList;
	}

	public ArrayList< T > getFixedValueList()
	{
		return fixedValueList;
	}
}
