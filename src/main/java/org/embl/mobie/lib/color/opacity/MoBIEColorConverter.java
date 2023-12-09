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
package org.embl.mobie.lib.color.opacity;

import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class MoBIEColorConverter< R extends RealType< ? > > implements OpacityAdjuster, ColorConverter, Converter< R, ARGBType >
{
	private double min = 0;

	private double max = 1;

	private final ARGBType color = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );

	private int A;

	private double scaleR;

	private double scaleG;

	private double scaleB;

	private double opacity = 1.0;

	private int background = 0;

	private boolean invert = false;

	public MoBIEColorConverter( final R type, final double min, final double max )
	{
		this.min = min;
		this.max = max;
		update();
	}

	@Override
	public void convert( R realType, ARGBType color )
	{
		if ( realType.getRealDouble() == background )
		{
			// For the accumulate projector to know where the source ends
			color.set( new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) ) );
		}
		else
		{
			final double v = invert ? max - realType.getRealDouble() : realType.getRealDouble() - min;
			int r0 = ( int ) ( scaleR * v + 0.5 );
			int g0 = ( int ) ( scaleG * v + 0.5 );
			int b0 = ( int ) ( scaleB * v + 0.5 );
			final int r = Math.max( Math.min( 255, r0 ), 0 );
			final int g = Math.max( Math.min( 255, g0 ), 0 );
			final int b = Math.max( Math.min( 255, b0 ), 0 );
			color.set( ARGBType.rgba( r, g, b, A ) );
			adjustOpacity( color, opacity );
		}
	}

	@Override
	public void setOpacity( double opacity )
	{
		this.opacity = opacity;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	public boolean isInvert()
	{
		return invert;
	}

	public void setInvert( boolean invert )
	{
		this.invert = invert;
	}

	@Override
	public ARGBType getColor()
	{
		return color.copy();
	}

	@Override
	public void setColor( final ARGBType c )
	{
		color.set( c );
		update();
	}

	@Override
	public boolean supportsColor()
	{
		return true;
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMax( final double max )
	{
		this.max = max;
		update();
	}

	@Override
	public void setMin( final double min )
	{
		this.min = min;
		update();
	}

	private void update()
	{
		final double scale = 1.0 / ( max - min );
		final int value = color.get();
		A = ARGBType.alpha( value );
		scaleR = ARGBType.red( value ) * scale;
		scaleG = ARGBType.green( value ) * scale;
		scaleB = ARGBType.blue( value ) * scale;
	}
}
