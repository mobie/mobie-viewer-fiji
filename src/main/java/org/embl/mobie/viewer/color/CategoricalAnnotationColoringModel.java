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

import org.embl.mobie.viewer.annotation.Annotation;

import org.embl.mobie.viewer.color.lut.LUTs;
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
			@Nullable String columnName,
			String lutName )
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
			convertStringToARGB( input.getValue( columnName ).toString(), output );
		else
			convertStringToARGB( input.toString(), output );

		adjustOpacity( output, opacity );
	}

	public void convertStringToARGB( String value, ARGBType output )
	{
		if ( inputToFixedColor.keySet().contains( value ) )
		{
			output.set( inputToFixedColor.get( value ) );
		}
 		else if ( inputToRandomColor.keySet().contains( value ) )
		{
			output.set( inputToRandomColor.get( value ) );
		}
		else // create and remember random color for this value
		{
			final double random = createRandom( value.hashCode() );
			final int argb = lut.getARGB( random );
			inputToRandomColor.put( value, argb );
			output.set( argb );
		}
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
		this.randomSeed = randomSeed;
	}

	public int getRandomSeed()
	{
		return randomSeed;
	}
}
