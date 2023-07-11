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
package org.embl.mobie.lib.color;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.color.opacity.OpacityAdjuster;

import static net.imglib2.type.numeric.ARGBType.alpha;
import static net.imglib2.type.numeric.ARGBType.blue;
import static net.imglib2.type.numeric.ARGBType.green;
import static net.imglib2.type.numeric.ARGBType.red;

public abstract class OpacityHelper
{
	private static void adjustColorConverterOpacity( SourceAndConverter< ? > sourceAndConverter, double opacity )
	{
		adjustOpacity( ( ColorConverter ) sourceAndConverter.getConverter(), opacity );

		if ( sourceAndConverter.asVolatile() != null )
		{
			adjustOpacity( ( ColorConverter ) sourceAndConverter.asVolatile().getConverter(), opacity );
		}
	}

	private static void adjustOpacityAdjusterOpacity( SourceAndConverter< ? > sourceAndConverter, double opacity )
	{
		( ( OpacityAdjuster ) sourceAndConverter.getConverter() ).setOpacity( opacity );

		if ( sourceAndConverter.asVolatile() != null )
		{
			( ( OpacityAdjuster ) sourceAndConverter.asVolatile().getConverter() ).setOpacity( opacity );
		}
	}

	private static void adjustOpacity( ColorConverter colorConverter, double opacity  )
	{
		colorConverter.getColor().get();
		final int value = colorConverter.getColor().get();
		colorConverter.setColor( new ARGBType( ARGBType.rgba( red( value ), green( value ), blue( value ), alpha( value ) * opacity ) ) );
	}

	public static double getOpacity( Converter< ?, ARGBType > converter )
	{
		if ( converter instanceof OpacityAdjuster )
		{
			return ( ( OpacityAdjuster ) converter ).getOpacity();
		}
		else if ( converter instanceof ColorConverter )
		{
			final ColorConverter colorConverter = ( ColorConverter ) converter;
			final int alpha = alpha( colorConverter.getColor().get() );
			return alpha / 255.0D;
		}
		else
		{
			throw new RuntimeException("Cannot determine opacity of " + converter.getClass() );
		}
	}

	public static void setOpacity( SourceAndConverter< ? > sourceAndConverter, double opacity )
	{
		final Converter< ?, ARGBType > converter = sourceAndConverter.getConverter();

		if ( converter instanceof OpacityAdjuster )
		{
			adjustOpacityAdjusterOpacity( sourceAndConverter, opacity );
		}
		else if ( converter instanceof ColorConverter )
		{
			adjustColorConverterOpacity( sourceAndConverter, opacity );
		}
		else
		{
			throw new UnsupportedOperationException("Cannot adjust opacity of converter: " + converter.getClass().getName() );
		}
	}
}
