/*-
 * #%L
 * TODO
 * %%
 * Copyright (C) 2018 - 2020 EMBL
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
package de.embl.cba.mobie.n5.source;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class LabelConverter implements Converter< RealType, ARGBType >, ColorConverter
{
	private final LazyCategoryColoringModel< Double > coloringModel;
	private ARGBType singleColor;
	private double max = 500;

	public LabelConverter()
	{
		this.coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
	}

	@Override
	public void convert( RealType label, ARGBType color )
	{
		if ( label instanceof Volatile )
		{
			if ( ! ( ( Volatile ) label ).isValid() )
			{
				color.set( 0 );
				return;
			}
		}

		final double realDouble = label.getRealDouble();

		if ( realDouble == 0 )
		{
			color.set( 0 );
			return;
		}

		if ( singleColor != null )
		{
			color.set( singleColor.get() );
			return;
		}

		coloringModel.convert( realDouble, color );
		color.mul( 100 / max ); // changes the intensity
	}

	public LazyCategoryColoringModel< Double > getColoringModel()
	{
		return coloringModel;
	}

	public void setSingleColor( ARGBType argbType )
	{
		singleColor = argbType;
	}

	@Override
	public ARGBType getColor()
	{
		return null;
	}

	@Override
	public void setColor( ARGBType c )
	{ }

	@Override
	public boolean supportsColor()
	{
		return false;
	}

	@Override
	public double getMin()
	{
		return 0;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMin( double min )
	{
	}

	@Override
	public void setMax( double max )
	{
		this.max = max;
	}
}
