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
package org.embl.mobie.viewer.color;

import net.imglib2.type.numeric.ARGBType;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ColorHelper
{
	public static Color getColor( ARGBType argbType )
	{
		final int colorIndex = argbType.get();

		return new Color(
				ARGBType.red( colorIndex ),
				ARGBType.green( colorIndex ),
				ARGBType.blue( colorIndex ),
				ARGBType.alpha( colorIndex ));
	}

	public static ARGBType getARGBType( Color color )
	{
		return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
	}

	public static Color getColor( String argbTypeToString ) {
		// ARGBType.toString()
		// i.e. "(r=255,g=255,b=255,a=255)"
		Pattern pattern = Pattern.compile("\\(r=(.+),g=(.+),b=(.+),a=(.+)\\)");
		Matcher matcher = pattern.matcher(argbTypeToString);
		if ( matcher.matches() )
		{
			Color color = new Color(Integer.parseInt(matcher.group(1)),
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)),
					Integer.parseInt(matcher.group(4)));
			return color;
		}
		else
		{
			// assume of form ".*r=255,g=255,b=255,a=255.*"
			pattern = Pattern.compile(".*r=(.+),g=(.+),b=(.+),a=(.+).*");
			matcher = pattern.matcher(argbTypeToString);
			if ( matcher.matches() )
			{
				Color color = new Color(Integer.parseInt(matcher.group(1)),
						Integer.parseInt(matcher.group(2)),
						Integer.parseInt(matcher.group(3)),
						Integer.parseInt(matcher.group(4)));
				return color;
			}
			else
			{
				try {
					return (Color) Color.class.getField(argbTypeToString.toUpperCase()).get(null);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
					return null;
				}
			}
		}
	}

	public static ARGBType getARGBType( String name ) {
		final Color color = getColor( name );
		if ( color == null ) return null;
		else return getARGBType( color );
	}
}
