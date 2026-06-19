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

import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.color.lut.SingleColorARGBLut;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class NumericAnnotationColoringModel< A extends Annotation > extends AbstractAnnotationColoringModel< A >
{
	public static final int ZERO_ARGB = ARGBType.rgba( 0, 0, 0, 0 );
	private Pair< Double, Double > contrastLimits;
	private final boolean isZeroTransparent;
	private boolean highValuesTransparent = false;

	public NumericAnnotationColoringModel(
			String columnName,
			String lutName,
			Pair< Double, Double > contrastLimits )
	{
		this.columnName = columnName;
		this.lut = LUTs.getLut( lutName );
		this.contrastLimits = contrastLimits;
		this.isZeroTransparent = LUTs.isZeroTransparent( lutName );
	}

	@Override
	public void convert( A annotation, ARGBType output )
	{
		final Number number = ( Number ) annotation.getValue( columnName );
		if ( number == null )
			output.set( ZERO_ARGB );
		else
			setColorLinearly( number.floatValue(), output );
	}
	
	public double getMin()
	{
		return contrastLimits.getA();
	}

	public double getMax()
	{
		return contrastLimits.getB();
	}

	public void setMin( double min )
	{
		contrastLimits = new ValuePair<>( min, contrastLimits.getB() );
		notifyColoringListeners();
	}

	public void setMax( double max )
	{
		contrastLimits = new ValuePair<>( contrastLimits.getA(), max );
		notifyColoringListeners();
	}

	public boolean isSingleColorLut()
	{
		return lut instanceof SingleColorARGBLut;
	}

	public ARGBType getSingleColor()
	{
		if ( ! isSingleColorLut() )
			return null;

		return new ARGBType( lut.getARGB( 1.0 ) );
	}

	public boolean isHighValuesTransparent() {
		return highValuesTransparent;
	}

	public void setHighValuesTransparent(boolean transparent) {
		this.highValuesTransparent = transparent;
		notifyColoringListeners();
	}

	public void setSingleColor( ARGBType color )
	{
		String lutName = LUTs.createSingleColorLutName( color );
		if ( isZeroTransparent )
			lutName += LUTs.ZERO_TRANSPARENT;

		lut = LUTs.getLut( lutName );
		notifyColoringListeners();
	}

	private void setColorLinearly( Float value, ARGBType output )
	{
		if ( isZeroTransparent )
		{
			if ( value == 0 )
			{
				output.set( ZERO_ARGB );
				return;
			}
		}

		if ( value.isNaN() )
		{
			output.set( ARGBType.rgba( 0, 0, 0, 0 ) );
			return;
		}

		// If enabled, treat values strictly greater than the contrast max as transparent
		if ( highValuesTransparent )
		{
			final double max = contrastLimits.getB();
			// If max is NaN or infinite we don't treat anything specially
			if ( Double.isFinite( max ) && value > max )
			{
				output.set( ZERO_ARGB );
				return;
			}
		}

		double normalisedValue = normalise( value );
		final int colorIndex = lut.getARGB( normalisedValue );
		output.set( colorIndex );
	}

	private double normalise( double value )
	{
		if ( contrastLimits.getA() == contrastLimits.getB() )
		{
			return 0.5; // TODO: be more sophisticated here?
		}
		else
		{
			return Math.max(
						Math.min(
							( value - contrastLimits.getA() )
							/ ( contrastLimits.getB() - contrastLimits.getA() ), 1.0 ), 0.0 );
		}
	}
}
