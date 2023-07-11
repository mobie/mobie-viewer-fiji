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
package org.embl.mobie.lib.color.lut;

import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;

/**
 * Modified version of Glasbey LUT,
 * excluding grayish and dark colors
 *
 */
public class GlasbeyARGBLut implements IndexARGBLut
{
	public static final int MINIMUM_RGB_DIFFERENCE = 50;
	public static final int MINIMUM_SUM_BRIGHTNESS = 150;

	private static int alpha;
	private static ArrayList< Integer > indices;
	private final int numColors;
	private String name = LUTs.GLASBEY;

	public GlasbeyARGBLut()
	{
		this( 255 );
	}

	public GlasbeyARGBLut( int alpha )
	{
		this.alpha = alpha;
		indices = this.argbGlasbeyIndices();
		numColors = indices.size();
	}

	@Override
	public int getARGB( double x )
	{
		final int index = ( int ) Math.ceil( x * ( numColors - 1 ) );
		return indices.get( index );
	}

	@Override
	public int getARGB( int i )
	{
		final int index = i % numColors;
		return indices.get( index );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Create lookup table with a  maximally distinct sets of colors (copied
	 * from Fiji's Glasbey LUT).
	 * Reference:
	 * [1] Glasbey, Chris, Gerie van der Heijden, Vivian FK Toh, and Alision
	 *     Gray. "Colour displays for categorical images." Color Research &amp;
	 *     Application 32, no. 4 (2007): 304-309.
	 *
	 * @return Glasbey lookup table
	 */
	public final static ArrayList< Integer > argbGlasbeyIndices()
	{
		int[] r = { 0, 0, 255, 0, 0, 255, 0, 255, 0, 154, 0, 120, 31, 255,
				177, 241, 254, 221, 32, 114, 118, 2, 200, 136, 255, 133, 161,
				20, 0, 220, 147, 0, 0, 57, 238, 0, 171, 161, 164, 255, 71, 212,
				251, 171, 117, 166, 0, 165, 98, 0, 0, 86, 159, 66, 255, 0, 252,
				159, 167, 74, 0, 145, 207, 195, 253, 66, 106, 181, 132, 96, 255,
				102, 254, 228, 17, 210, 91, 32, 180, 226, 0, 93, 166, 97, 98,
				126, 0, 255, 7, 180, 148, 204, 55, 0, 150, 39, 206, 150, 180,
				110, 147, 199, 115, 15, 172, 182, 216, 87, 216, 0, 243, 216, 1,
				52, 255, 87, 198, 255, 123, 120, 162, 105, 198, 121, 0, 231, 217,
				255, 209, 36, 87, 211, 203, 62, 0, 112, 209, 0, 105, 255, 233,
				191, 69, 171, 14, 0, 118, 255, 94, 238, 159, 80, 189, 0, 88, 71,
				1, 99, 2, 139, 171, 141, 85, 150, 0, 255, 222, 107, 30, 173,
				255, 0, 138, 111, 225, 255, 229, 114, 111, 134, 99, 105, 200,
				209, 198, 79, 174, 170, 199, 255, 146, 102, 111, 92, 172, 210,
				199, 255, 250, 49, 254, 254, 68, 201, 199, 68, 147, 22, 8, 116,
				104, 64, 164, 207, 118, 83, 0, 43, 160, 176, 29, 122, 214, 160,
				106, 153, 192, 125, 149, 213, 22, 166, 109, 86, 255, 255, 255,
				202, 67, 234, 191, 38, 85, 121, 254, 139, 141, 0, 63, 255, 17,
				154, 149, 126, 58, 189 };
		int[] g = { 0, 0, 0, 255, 0, 0, 83, 211, 159, 77, 255, 63, 150, 172,
				204, 8, 143, 0, 26, 0, 108, 173, 255, 108, 183, 133, 3, 249, 71,
				94, 212, 76, 66, 167, 112, 0, 245, 146, 255, 206, 0, 173, 118,
				188, 0, 0, 115, 93, 132, 121, 255, 53, 0, 45, 242, 93, 255, 191,
				84, 39, 16, 78, 149, 187, 68, 78, 1, 131, 233, 217, 111, 75,
				100, 3, 199, 129, 118, 59, 84, 8, 1, 132, 250, 123, 0, 190, 60,
				253, 197, 167, 186, 187, 0, 40, 122, 136, 130, 164, 32, 86, 0,
				48, 102, 187, 164, 117, 220, 141, 85, 196, 165, 255, 24, 66,
				154, 95, 241, 95, 172, 100, 133, 255, 82, 26, 238, 207, 128,
				211, 255, 0, 163, 231, 111, 24, 117, 176, 24, 30, 200, 203, 194,
				129, 42, 76, 117, 30, 73, 169, 55, 230, 54, 0, 144, 109, 223,
				80, 93, 48, 206, 83, 0, 42, 83, 255, 152, 138, 69, 109, 0, 76,
				134, 35, 205, 202, 75, 176, 232, 16, 82, 137, 38, 38, 110, 164,
				210, 103, 165, 45, 81, 89, 102, 134, 152, 255, 137, 34, 207,
				185, 148, 34, 81, 141, 54, 162, 232, 152, 172, 75, 84, 45, 60,
				41, 113, 0, 1, 0, 82, 92, 217, 26, 3, 58, 209, 100, 157, 219,
				56, 255, 0, 162, 131, 249, 105, 188, 109, 3, 0, 0, 109, 170,
				165, 44, 185, 182, 236, 165, 254, 60, 17, 221, 26, 66, 157,
				130, 6, 117};
		int[] b = { 0, 255, 0, 0, 51, 182, 0, 0, 255, 66, 190, 193, 152, 253,
				113, 92, 66, 255, 1, 85, 149, 36, 0, 0, 159, 103, 0, 255, 158,
				147, 255, 255, 80, 106, 254, 100, 204, 255, 115, 113, 21, 197,
				111, 0, 215, 154, 254, 174, 2, 168, 131, 0, 63, 66, 187, 67,
				124, 186, 19, 108, 166, 109, 0, 255, 64, 32, 0, 84, 147, 0, 211,
				63, 0, 127, 174, 139, 124, 106, 255, 210, 20, 68, 255, 201, 122,
				58, 183, 0, 226, 57, 138, 160, 49, 1, 129, 38, 180, 196, 128,
				180, 185, 61, 255, 253, 100, 250, 254, 113, 34, 103, 105, 182,
				219, 54, 0, 1, 79, 133, 240, 49, 204, 220, 100, 64, 70, 69, 233,
				209, 141, 3, 193, 201, 79, 0, 223, 88, 0, 107, 197, 255, 137,
				46, 145, 194, 61, 25, 127, 200, 217, 138, 33, 148, 128, 126, 96,
				103, 159, 60, 148, 37, 255, 135, 148, 0, 123, 203, 200, 230, 68,
				138, 161, 60, 0, 157, 253, 77, 57, 255, 101, 48, 80, 32, 0, 255,
				86, 77, 166, 101, 175, 172, 78, 184, 255, 159, 178, 98, 147, 30,
				141, 78, 97, 100, 23, 84, 240, 0, 58, 28, 121, 0, 255, 38, 215,
				155, 35, 88, 232, 87, 146, 229, 36, 159, 207, 105, 160, 113, 207,
				89, 34, 223, 204, 69, 97, 78, 81, 248, 73, 35, 18, 173, 0, 51,
				2, 158, 212, 89, 193, 43, 40, 246, 146, 84, 238, 72, 101, 101 };


		indices = new ArrayList< Integer >();

		indices.add( ARGBType.rgba( 0, 0, 0, 0) );

		for (int i = 1; i < r.length; i++)
		{
			if ( ( Math.abs( g[ i ] - r[ i ] ) < MINIMUM_RGB_DIFFERENCE )
					&& ( Math.abs( g[ i ] - b[ i ] ) < MINIMUM_RGB_DIFFERENCE )
					&& ( Math.abs( r[ i ] - b[ i ] ) < MINIMUM_RGB_DIFFERENCE ) )
				continue; // too grayish

			if ( ( r[ i ] + g[ i ] + b[ i ] ) < MINIMUM_SUM_BRIGHTNESS )
				continue; // too dark

			indices.add( ARGBType.rgba(
					r[ i ],
					g[ i ],
					b[ i ],
					alpha ));
		}

		return indices;
	}


}
