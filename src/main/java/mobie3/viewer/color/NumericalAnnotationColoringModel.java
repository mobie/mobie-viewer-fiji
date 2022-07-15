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

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.tables.color.ARBGLutSupplier;
import de.embl.cba.tables.color.AbstractColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import mobie3.viewer.annotation.Annotation;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

// TODO: extract abstract class NumericFeatureColoringModel
public class NumericalAnnotationColoringModel< A extends Annotation >
		extends AbstractColoringModel< A > implements NumericColoringModel< A >, ColumnColoringModel, ARBGLutSupplier
{
	private final String columnName;
	private final ARGBLut lut;
	private Pair< Double, Double > contrastLimits;
	private Pair< Double, Double > range;
	// TODO: also capture this with inputToFixedColor logic
	private final boolean isZeroTransparent;

	public NumericalAnnotationColoringModel(
			String columnName,
			ARGBLut lut,
			Pair< Double, Double > range,
			boolean isZeroTransparent )
	{
		this.columnName = columnName;
		this.lut = lut;
		this.contrastLimits = new ValuePair<>( range.getA(), range.getB() );
		this.range = range;
		this.isZeroTransparent = isZeroTransparent;
	}

	@Override
	public void convert( A tableRow, ARGBType output )
	{
		final Double value = ( Double ) tableRow.getValue( columnName );
		setColorLinearly( value, output );
	}

	@Override
	public double getMin()
	{
		return contrastLimits.getA();
	}


	@Override
	public double getMax()
	{
		return contrastLimits.getB();
	}


	@Override
	public void setMin( double min )
	{
		contrastLimits = new ValuePair<>( min, contrastLimits.getB() );
		notifyColoringListeners();
	}

	@Override
	public void setMax( double max )
	{
		contrastLimits = new ValuePair<>( contrastLimits.getA(), max );
		notifyColoringListeners();
	}

	private void setColorLinearly( Double value, ARGBType output )
	{
		if ( isZeroTransparent )
		{
			if ( value == 0 )
			{
				output.set( ARGBType.rgba( 0, 0, 0, 0 ) );
				return;
			}
		}

		if ( value.isNaN() )
		{
			output.set( ARGBType.rgba( 0, 0, 0, 0 ) );
			return;
		}

		double normalisedValue = normalise( value );
		final int colorIndex = lut.getARGB( normalisedValue );
		output.set( colorIndex );
	}

	private double normalise( double value )
	{
		if ( contrastLimits.getA() == contrastLimits.getB() )
		{
			if ( contrastLimits.getB() == range.getA() )
				return 1.0;
			else if ( contrastLimits.getB() == range.getB() )
				return 0.0;
			else
				return 0.0;
		}
		else
		{
			return Math.max(
						Math.min(
							( value - contrastLimits.getA() )
							/ ( contrastLimits.getB() - contrastLimits.getA() ), 1.0 ), 0.0 );
		}
	}

	@Override
	public String getColumnName()
	{
		return columnName;
	}

	@Override
	public ARGBLut getARGBLut() {
		return this.lut;
	}
}
