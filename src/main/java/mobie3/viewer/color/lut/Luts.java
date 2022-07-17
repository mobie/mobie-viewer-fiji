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
package mobie3.viewer.color.lut;
import ij.process.LUT;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;

public class Luts
{
	public static final byte[][] GLASBEY = glasbeyLut();
	public static final byte[][] GOLDEN_ANGLE = goldenAngleLut();
	public static final byte[][] GRAYSCALE = grayscaleLut();
	public static final byte[][] BLUE_WHITE_RED = blueWhiteRedLut();

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
	private final static byte[][] glasbeyLut() {
		// initial values (copied from Fiji's Glasbey LUT)
		int[] r = getGlasbeyRed();
		int[] g = getGlasbeyGreen();
		int[] b = getGlasbeyBlue();

		// create map
		byte[][] map = new byte[r.length][3];

		// cast elements
		for ( int i = 0; i < r.length; i++)
		{
			map[i][0] = (byte) r[i];
			map[i][1] = (byte) g[i];
			map[i][2] = (byte) b[i];
		}

		return map;
	}


	public final static LUT glasbeyLutIJ() {
		// initial values (copied from Fiji's Glasbey LUT)
		int[] r = getGlasbeyRed();
		int[] g = getGlasbeyGreen();
		int[] b = getGlasbeyBlue();

		// create map
		byte[][] map = new byte[3][r.length];

		// cast elements
		for ( int i = 0; i < r.length; i++)
		{
			map[0][i] = (byte) r[i];
			map[1][i] = (byte) g[i];
			map[2][i] = (byte) b[i];
		}

		return new LUT( map[ 0 ], map[ 1 ], map[ 2 ] );
	}

	private static int[] getGlasbeyBlue()
	{
		return new int[]{ 0, 255, 0, 0, 51, 182, 0, 0, 255, 66, 190, 193, 152, 253,
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
	}

	private static int[] getGlasbeyRed()
	{
		return new int[]{ 0, 0, 255, 0, 0, 255, 0, 255, 0, 154, 0, 120, 31, 255,
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
	public final static int[] argbGlasbeyIndicies()
	{
		int[] r = getGlasbeyRed();
		int[] g = getGlasbeyGreen();
		int[] b = getGlasbeyBlue();

		int[] argbIndicies = new int[ r.length ];

		for (int i = 0; i < r.length; i++)
		{
			argbIndicies[ i ] = ARGBType.rgba(
					( byte ) r[ i ],
					( byte ) g[ i ],
					( byte ) b[ i ],
					255 );
		}

		return argbIndicies;
	}

	public static int[] getGlasbeyGreen()
	{
		return new int[]{ 0, 0, 0, 255, 0, 0, 83, 211, 159, 77, 255, 63, 150, 172,
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
	}


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

	public static int getARGBIndex( final byte lutIndex, final byte[][] lut )
	{
		final int color = ARGBType.rgba(
				( lut[ lutIndex & 0xFF ][ 0 ] & 0xFF ) ,
				( lut[ lutIndex & 0xFF ][ 1 ] & 0xFF ),
				( lut[ lutIndex & 0xFF ][ 2 ] & 0xFF ), 255 );

		return color;
	}


	/**
	 * Make lookup table with esthetically pleasing colors based on the golden
	 * angle
	 *
	 * Taken from: MorphoLibJ
	 *
	 * @return lookup table with golden-angled-based colors
	 */
	private final static byte[][] goldenAngleLut( )
	{

		// hue for assigning new color ([0.0-1.0])
		float hue = 0.5f;
		// saturation for assigning new color ([0.5-1.0])
		float saturation = 0.75f;

		// create colors recursively by adding golden angle ratio to hue and
		// saturation of previous color
		Color[] colors = new Color[256];
		for (int i = 0; i < 256; i++)
		{
			// create current color
			colors[i] = Color.getHSBColor(hue, saturation, 1);

			// update hue and saturation for next color
			hue += 0.38197f; // golden angle
			if (hue > 1)
				hue -= 1;
			saturation += 0.38197f; // golden angle
			if (saturation > 1)
				saturation -= 1;
			saturation = 0.5f * saturation + 0.5f;
		}

		// create map
		byte[][] lut = new byte[256][3];

		// fill up the color map by converting color array
		for (int i = 0; i < 256; i++)
		{
			Color color = colors[i];
			lut[i][0] = (byte) color.getRed();
			lut[i][1] = (byte) color.getGreen();
			lut[i][2] = (byte) color.getBlue();
		}

		return lut;
	}


	private final static byte[][] grayscaleLut( )
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

	public final static byte[][] colorLut( Integer color )
	{
		byte[][] lut = new byte[256][3];

		for (int i = 0; i < 256; i++)
		{
			lut[i][0] = (byte) ( ARGBType.red ( color ) * i / 255.0 ); // red
			lut[i][1] = (byte) ( ARGBType.green ( color ) * i / 255.0 ); // green
			lut[i][2] = (byte) ( ARGBType.blue ( color ) * i / 255.0 ); // blue
		}

		return lut;
	}


	private final static byte[][] blueWhiteRedLut( )
	{

		byte[][] lut = new byte[256][3];

		int[] blue = new int[]{ 0, 0, 255 };
		int[] white = new int[]{ 255, 255, 255 };
		int[] red = new int[]{ 255, 0, 0 };

		final int middle = 256 / 2;

		for ( int i = 0; i < middle; i++)
		{
			for ( int j = 0; j < 3; j++ )
			{
				lut[ i ][ j ] = ( byte ) ( blue[ j ] + ( 1.0 * i / middle ) * ( white[ j ] - blue[ j ] ) );
			}
		}

		for ( int i = middle; i < 256; i++)
		{
			for ( int j = 0; j < 3; j++ )
			{
				lut[ i ][ j ] = ( byte ) ( white[ j ] + ( 1.0 * ( i - middle ) / middle ) * ( red[ j ] - white[ j ] ) );
			}
		}


		return lut;
	}
}
