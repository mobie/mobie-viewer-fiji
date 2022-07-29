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
package org.embl.mobie3.viewer.color.lut;

import net.imglib2.type.numeric.ARGBType;

public class BlueWhiteRedARGBLut implements ARGBLut
{
	public static final int NUM_COLORS = 255;
	private static int alpha;
	private final int[] indices;
	private String name = LUTs.BLUE_WHITE_RED;

	public BlueWhiteRedARGBLut( )
	{
		alpha = 255;
		indices = this.blueWhiteRedARGBIndices( NUM_COLORS );
	}

	public BlueWhiteRedARGBLut( int alpha )
	{
		this.alpha = alpha;
		indices = this.blueWhiteRedARGBIndices( NUM_COLORS );
	}

	@Override
	public int getARGB( double x )
	{
		final int index = ( int ) ( x * ( NUM_COLORS - 1 ) );
		if ( index < 0 || index > indices.length -1 )
		{
			return indices[ 0 ];
		}
		return indices[ index ];
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Lookup table going from blue to white to red.
	 *
	 *
	 * @param
	 * 		numColors
	 * @return
	 * 		ARGB indices, encoding the colors
	 */
	private final static int[] blueWhiteRedARGBIndices( int numColors )
	{
		int[][] lut = new int[ 3 ][ numColors ];

		int[] blue = new int[]{ 0, 0, 255 };
		int[] white = new int[]{ 255, 255, 255 };
		int[] red = new int[]{ 255, 0, 0 };

		final int middle = numColors / 2;

		for ( int i = 0; i < middle; i++)
		{
			for ( int rgb = 0; rgb < 3; rgb++ )
			{
				lut[ rgb ][ i ] = (int) ( blue[ rgb ] + ( 1.0 * i / middle ) * ( white[ rgb ] - blue[ rgb ] ) );
			}
		}

		for ( int i = middle; i < numColors; i++)
		{
			for ( int rgb = 0; rgb < 3; rgb++ )
			{
				lut[ rgb ][ i ] = ( int ) ( white[ rgb ] + ( 1.0 * ( i - middle ) / middle ) * ( red[ rgb ] - white[ rgb ] ) );
			}
		}

		int[] argbIndices = new int[ numColors ];

		for (int i = 0; i < numColors; i++)
		{
			argbIndices[ i ] = ARGBType.rgba(
					lut[ 0 ][ i ],
					lut[ 1 ][ i ],
					lut[ 2 ][ i ],
					alpha );
		}

		return argbIndices;
	}

}
