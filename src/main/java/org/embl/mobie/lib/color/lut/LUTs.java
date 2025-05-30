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
package org.embl.mobie.lib.color.lut;

import net.imglib2.type.numeric.ARGBType;

public class LUTs
{
	public static final String BLUE_WHITE_RED = "blueWhiteRed";
	public static final String VIRIDIS = "viridis";
	public static final String GLASBEY = "glasbey";
	public static final String GRAYSCALE = "grayscale";
	public static final String ARGB_COLUMN = "argbColumn"; // https://github.com/mobie/mobie.github.io/issues/100#issuecomment-1405459668
	public static final String RGBA_COLUMN = "rgbaColumn";
	public static final String ZERO_TRANSPARENT = "ZeroTransparent";

	public static final String[] COLORING_LUTS = new String[]
	{
		BLUE_WHITE_RED,
		VIRIDIS,
		GLASBEY,
		RGBA_COLUMN
	};

	public static final byte[][] GRAYSCALE_LUT = grayscaleLut();

	public static final ARGBType TRANSPARENT = new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
	public static final ARGBType DARK_GREY = new ARGBType( ARGBType.rgba( 100, 100, 100, 255 ) );

	public static int getARGBIndex( final byte lutIndex, final byte[][] lut, double brightness )
	{
		final int color = ARGBType.rgba(
				( lut[ lutIndex & 0xFF ][ 0 ] & 0xFF ) * brightness ,
				( lut[ lutIndex & 0xFF ][ 1 ] & 0xFF ) * brightness,
				( lut[ lutIndex & 0xFF ][ 2 ] & 0xFF ) * brightness, 255 );

		return color;
	}

	/**
	 *
	 * @param r 0 - 255
	 * @param g 0 - 255
	 * @param b 0 - 255
	 * @param brightness 0.0 - 1.0
	 * @return
	 */
	public static int getARGBIndex( int r, int g, int b, double brightness )
	{
		final int color = ARGBType.rgba(
				(int) (r * brightness),
				(int) (g * brightness),
				(int) (b * brightness),
				(int) (255 * brightness) );

		return color;
	}

	public static ARGBLut getLut( String lutName )
	{
		// we also encode zeroTransparent in the lutName
		// thus we need contains instead of equals
		ARGBLut lut;

		if ( lutName.contains( BLUE_WHITE_RED ) )
		{
			lut = new BlueWhiteRedARGBLut();
		}
		else if ( lutName.contains( VIRIDIS ) )
		{
			lut = new ViridisARGBLut();
		}
		else if ( lutName.contains( GLASBEY ) )
		{
			lut = new GlasbeyARGBLut();
		}
		else if ( lutName.contains( ARGB_COLUMN ) || lutName.contains( RGBA_COLUMN ) )
		{
			lut = new ColumnARGBLut();
		}
		else
		{
			throw new UnsupportedOperationException( "LUT " + lutName + " is not supported." );
		}

		lut.setName( lutName ); // (keep zeroTransparent encoding)

		return lut;
	}

	public static boolean isZeroTransparent( String lutName )
	{
		return lutName.contains( ZERO_TRANSPARENT );
	}

	private static byte[][] grayscaleLut( )
	{
		byte[][] lut = new byte[256][3];

		// fill up the color map by converting color array
		for (int i = 0; i < 256; i++)
		{
			lut[i][0] = (byte) i; // red
			lut[i][1] = (byte) i; // green
			lut[i][2] = (byte) i; // blue
		}

		return lut;
	}
}
