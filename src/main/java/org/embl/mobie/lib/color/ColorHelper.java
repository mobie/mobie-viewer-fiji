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
package org.embl.mobie.lib.color;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import ij.process.LUT;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ColorHelper
{
	final static public double goldenRatio = 1.0 / ( 0.5 * Math.sqrt( 5 ) + 0.5 );
	public static final String RANDOM_FROM_GLASBEY = "randomFromGlasbey";

	public static Color getColor( ARGBType argbType )
	{
		if ( argbType == null )
			return null;

		final int colorIndex = argbType.get();

		return new Color(
				ARGBType.red( colorIndex ),
				ARGBType.green( colorIndex ),
				ARGBType.blue( colorIndex ),
				ARGBType.alpha( colorIndex ));
	}

	public static String getString( int[] ints )
	{
		return getString( getARGBType( ints ) );
	}

	public static String getString( LUT lut )
	{
		return getString( getARGBType( lut ) );
	}

	// convert a color to a string
	// https://github.com/mobie/mobie-viewer-fiji/issues/924
	public static String getString( ARGBType argbType )
	{
		if ( argbType == null ) return null;

		final int colorIndex = argbType.get();
		final String string = "r" + ARGBType.red( colorIndex ) + "-g" + ARGBType.green( colorIndex ) + "-b" + ARGBType.blue( colorIndex ) + "-a" + ARGBType.alpha( colorIndex );
		return string;
	}

	public static ARGBType getARGBType( Color color )
	{
		return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
	}

	public static Color getColor( String string ) {
		return getColor( getARGBType( string ) );
	}

	// convert a string to a color
	// trying various encodings
	// https://github.com/mobie/mobie-viewer-fiji/issues/924
	public static ARGBType getARGBType( String string )
	{
		if ( string == null ) return null;
		if ( string.equals( "" ) ) return null;

		Pattern pattern = Pattern.compile("r(.+)-g(.+)-b(.+)-a(.+)");
		Matcher matcher = pattern.matcher(string);
		if ( matcher.matches() )
			return getArgbType( matcher );

		pattern = Pattern.compile("(.+)-(.+)-(.+)-(.+)");
		matcher = pattern.matcher(string);
		if ( matcher.matches() )
			return getArgbType( matcher );

		pattern = Pattern.compile(".*r=(.+),g=(.+),b=(.+),a=(.+).*");
		matcher = pattern.matcher(string);
		if ( matcher.matches() )
			return getArgbType( matcher );

		try {
			final Color color = ( Color ) Color.class.getField( string.toUpperCase() ).get( null );
			return getARGBType( color );
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	private static ARGBType getArgbType( Matcher matcher )
	{
		return new ARGBType(
						ARGBType.rgba(
								Integer.parseInt(matcher.group(1)),
								Integer.parseInt(matcher.group(2)),
								Integer.parseInt(matcher.group(3)),
								Integer.parseInt(matcher.group(4))
								));
	}

	public static ARGBType getARGBType( int[] rgba )
	{
		return new ARGBType( ARGBType.rgba( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] ) );
	}

	public static ARGBType getARGBType( LUT lut )
	{
		return getARGBType( getRGBA( lut ) );
	}

	private static int[] getRGBA( LUT lut )
	{
		return new int[]{ lut.getRed( 255 ), lut.getGreen( 255 ), lut.getBlue( 255 ), lut.getAlpha( 255 ) };
	}

	public static ARGBType getRandomGlasbeyARGBType( String name )
	{
		final GlasbeyARGBLut glasbeyARGBLut = new GlasbeyARGBLut();
		double random = name.hashCode() * goldenRatio;
		random = random - ( long ) Math.floor( random );
		final int argb = glasbeyARGBLut.getARGB( random );
		final ARGBType argbType = new ARGBType( argb );
		return argbType;
	}
}
