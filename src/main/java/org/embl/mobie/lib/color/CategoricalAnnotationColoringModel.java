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

import org.embl.mobie.lib.annotation.Annotation;

import org.embl.mobie.lib.color.lut.ColumnARGBLut;
import org.embl.mobie.lib.color.lut.LUTs;
import net.imglib2.type.numeric.ARGBType;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class CategoricalAnnotationColoringModel< A extends Annotation > extends AbstractAnnotationColoringModel< A >
{
	private Map< String, Integer > inputToFixedColor;
	private Map< String, Integer > inputToRandomColor;
	private int randomSeed;

	public CategoricalAnnotationColoringModel(
			final String columnName, @Nullable
			final String lutName )
	{
		this.columnName = columnName;
		this.lut = LUTs.getLut( lutName );
		this.inputToRandomColor = new ConcurrentHashMap< String, Integer >(  );
		this.inputToFixedColor = new ConcurrentHashMap< String, Integer >(  );
		this.randomSeed = 50;

		if ( LUTs.isZeroTransparent( lutName ) )
		{
			this.assignColor( "0", LUTs.TRANSPARENT.get() );
			this.assignColor( "0.0", LUTs.TRANSPARENT.get() );
		}
	}

	@Override
	public void convert( A input, ARGBType output )
	{
		if ( columnName != null )
		{
			final Object value = input.getValue( columnName );
			if ( value == null )
				output.setZero();
			else
				convertStringToARGB( value.toString(), output );
		}
		else
		{
			convertStringToARGB( input.uuid(), output );
		}
	}

	public void convertStringToARGB( String categoricalValue, ARGBType output )
	{
		// fixed color
		//
		if ( inputToFixedColor.keySet().contains( categoricalValue ) )
		{
			output.set( inputToFixedColor.get( categoricalValue ) );
			return;
		}

		if ( lut instanceof ColumnARGBLut )
		{
			final ARGBType argbType = ColorHelper.getARGBType( categoricalValue );

			final int argbIndex;
			if ( argbType == null )
				argbIndex = LUTs.TRANSPARENT.get();
			else
				argbIndex = argbType.get();

			inputToFixedColor.put( categoricalValue, argbIndex );
			output.set( argbIndex );
			return;
		}

		// random color
		//
 		if ( inputToRandomColor.keySet().contains( categoricalValue ) )
		{
			output.set( inputToRandomColor.get( categoricalValue ) );
			return;
		}

		final double random = createRandom( categoricalValue.hashCode() );
		final int argb = lut.getARGB( random );
		inputToRandomColor.put( categoricalValue, argb );
		output.set( argb );
	}

	private double createRandom( double x )
	{
		double random = ( x * randomSeed ) * goldenRatio;
		random = random - ( long ) Math.floor( random );
		return random;
	}

	public void assignColor( String category, int color )
	{
		inputToFixedColor.put( category, color );
		notifyColoringListeners();
	}

	public void setRandomSeed( int randomSeed )
	{
		if ( randomSeed == this.randomSeed )
			return;

		inputToRandomColor.clear();
		this.randomSeed = randomSeed;
		notifyColoringListeners();
	}

	public int getRandomSeed()
	{
		return randomSeed;
	}
}
