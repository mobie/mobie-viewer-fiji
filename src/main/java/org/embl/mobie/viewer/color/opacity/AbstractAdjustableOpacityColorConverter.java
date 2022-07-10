/*-
 * #%L
 * Fiji viewer for MoBIE projects
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
package org.embl.mobie.viewer.color.opacity;

import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.color.OpacityAdjuster;

public abstract class AbstractAdjustableOpacityColorConverter< T > implements OpacityAdjuster, ColorConverter, Converter< T, ARGBType >
{
	protected Converter< RealType, ARGBType > converter;
	protected ColorConverter colorConverter;
	protected double opacity = 1.0;

	protected void setConverter( RealType type )
	{
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		this.converter = RealARGBColorConverter.create( type, typeMin, typeMax );
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

	@Override
	public ARGBType getColor()
	{
		return (( ColorConverter ) converter).getColor();
	}

	@Override
	public void setColor( ARGBType c )
	{
		(( ColorConverter ) converter).setColor( c );
	}

	@Override
	public boolean supportsColor()
	{
		return (( ColorConverter ) converter).supportsColor();
	}

	@Override
	public double getMin()
	{
		return (( ColorConverter ) converter).getMin();
	}

	@Override
	public double getMax()
	{
		return (( ColorConverter ) converter).getMax();
	}

	@Override
	public void setMin( double min )
	{
		(( ColorConverter ) converter).setMin( min );
	}

	@Override
	public void setMax( double max )
	{
		(( ColorConverter ) converter).setMax( max );
	}
}
