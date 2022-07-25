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
package mobie3.viewer.color;

import mobie3.viewer.color.lut.ARGBLut;
import mobie3.viewer.color.lut.BlueWhiteRedARGBLut;
import mobie3.viewer.color.lut.GlasbeyARGBLut;
import mobie3.viewer.color.lut.ViridisARGBLut;
import net.imglib2.type.numeric.ARGBType;

public class LUTs
{
	public static final String BLUE_WHITE_RED = "blueWhiteRed";
	public static final String VIRIDIS = "viridis";
	public static final String GLASBEY = "glasbey";
	public static final String ARGB_COLUMN = "argbColumn";

	public static final String ZERO_TRANSPARENT = "ZeroTransparent";

	public static final String[] COLORING_LUTS = new String[]
	{
		BLUE_WHITE_RED,
		VIRIDIS,
		GLASBEY,
		ARGB_COLUMN
	};
	public static final ARGBType TRANSPARENT = new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
	public static final ARGBType DARK_GREY = new ARGBType( ARGBType.rgba( 100, 100, 100, 255 ) );

	public static boolean isNumeric( String lut )
	{
		return lut.contains( BLUE_WHITE_RED )
				|| lut.contains( VIRIDIS );
	}

	public static boolean isCategorical( String lut )
	{
		return lut.contains( GLASBEY ) || lut.contains( ARGB_COLUMN );
	}


	public static ARGBLut getLut( String lutName )
	{
		// we also encode zeroTransparent in the lutName
		// thus we need contains instead of equals
		if ( lutName.contains( BLUE_WHITE_RED ) )
		{
			return new BlueWhiteRedARGBLut();
		}
		else if ( lutName.contains( VIRIDIS ) )
		{
			return new ViridisARGBLut();
		}
		else if ( lutName.contains( GLASBEY ) )
		{
			return new GlasbeyARGBLut();
		}
		else
		{
			throw new UnsupportedOperationException( "LUT " + lutName + " is not supported." );
		}
	}

	public static boolean isZeroTransparent( String lutName )
	{
		return lutName.contains( ZERO_TRANSPARENT );
	}

}
