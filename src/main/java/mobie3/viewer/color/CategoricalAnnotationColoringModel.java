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
import mobie3.viewer.table.Annotation;
import net.imglib2.type.numeric.ARGBType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class CategoricalAnnotationColoringModel< A extends Annotation >
		extends AbstractColoringModel< A > implements CategoricalColoringModel< A >, ColumnColoringModel, ARBGLutSupplier
{
	public static final ARGBType TRANSPARENT = new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
	public static final ARGBType DARK_GREY = new ARGBType( ARGBType.rgba( 100, 100, 100, 255 ) );

	// TODO: The maps could go to int instead of ARGBType
	private Map< Object, ARGBType > inputToFixedColor;
	private Map< Object, ARGBType > inputToRandomColor;
	private final String columnName;
	private ARGBLut argbLut;
	private int randomSeed;
	private boolean fixedColorMode = false;

	/**
	 *
	 * @param argbLut
	 */
	public CategoricalAnnotationColoringModel( String columnName, ARGBLut argbLut )
	{
		this.columnName = columnName;
		this.argbLut = argbLut;
		this.inputToRandomColor = new ConcurrentHashMap<>(  );
		this.inputToFixedColor = new ConcurrentHashMap<>(  );
		this.randomSeed = 50;
	}

	@Override
	public void convert( A input, ARGBType output )
	{
		final Object value = input.getValue( columnName );

		if ( inputToFixedColor.keySet().contains( value ) )
		{
			final int color = inputToFixedColor.get( value ).get();
			output.set( color );
		}
 		else if ( inputToRandomColor.keySet().contains( value ) )
		{
			final int color = inputToRandomColor.get( value ).get();
			output.set( color );
		}
		else
		{
			// final double random = createRandom( inputToRandomColor.size() + 1 );
			final double random = createRandom( value.hashCode() );
			final int color = argbLut.getARGB( random );
			inputToRandomColor.put( value, new ARGBType( color ) );
			output.set( color );
			return;
		}
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
		if ( fixedColorMode ) return;
		inputToRandomColor.clear();
		this.randomSeed++;
		notifyColoringListeners();
	}

	@Override
	public void setRandomSeed( int seed )
	{
		if ( fixedColorMode ) return;
		inputToRandomColor.clear();
		randomSeed = seed;
		notifyColoringListeners();
	}

	@Override
	public int getRandomSeed()
	{
		return randomSeed;
	}

	public void fixedColorMode( boolean fixedColorMode )
	{
		this.fixedColorMode = fixedColorMode;
	}

	public void putInputToFixedColor( Object input, ARGBType color )
	{
		inputToFixedColor.put( input, color );
		notifyColoringListeners();
	}

	public void removeInputToFixedColor( Object input )
	{
		inputToFixedColor.remove( input );
		notifyColoringListeners();
	}

	public String getColumnName()
	{
		return columnName;
	}

	public ARGBLut getARGBLut() {
		return this.argbLut;
	}
}
