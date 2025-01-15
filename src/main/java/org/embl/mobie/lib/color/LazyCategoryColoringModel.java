/*-
 * #%L
 * Various Java code for ImageJ
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

import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.color.lut.ARGBLut;

import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.lib.color.ColorHelper.goldenRatio;

public class LazyCategoryColoringModel< T > extends AbstractColoringModel< T > implements CategoryColoringModel< T >, ARBGLutSupplier
{
	private Map< T, ARGBType > inputToColorMap;
	private ARGBLut argbLut;
	private int randomSeed;

	/**
	 * Colors are lazily assigned to input elements,
	 * using the given {@code argbLut}.
	 *
	 * TODO: better to use here a "generating LUT" rather than a 0...1 LUT
	 *
	 * @param argbLut
	 */
	public LazyCategoryColoringModel( ARGBLut argbLut )
	{
		super();
		this.argbLut = argbLut;
		this.inputToColorMap = new HashMap<>(  );
		this.randomSeed = 42;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		if( ! inputToColorMap.keySet().contains( input ) )
		{
			final double random = createRandom( inputToColorMap.size() + 1 );
			inputToColorMap.put( input, new ARGBType( argbLut.getARGB( random ) ) );
		}

		output.set( inputToColorMap.get( input ).get() );
	}

	private double createRandom( double x )
	{
		double random = ( x * randomSeed ) * goldenRatio;
		random = random - ( long ) Math.floor( random );
		return random;
	}

	@Override
	public void incRandomSeed( )
	{
		inputToColorMap.clear();
		this.randomSeed++;
		notifyColoringListeners();
	}

	@Override
	public void setRandomSeed( int seed )
	{
		inputToColorMap.clear();
		randomSeed = seed;
		notifyColoringListeners();
	}

	@Override
	public int getRandomSeed()
	{
		return randomSeed;
	}

	@Override
	public ARGBLut getARGBLut()
	{
		return argbLut;
	}
}
