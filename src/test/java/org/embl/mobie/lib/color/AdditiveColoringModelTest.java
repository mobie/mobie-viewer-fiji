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

import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdditiveColoringModelTest
{
	@Test
	void blendsColorsAdditively()
	{
		final ArrayList< ColoringModel< Object > > colorings = new ArrayList<>();
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 255, 0, 255, 255 ) ) );
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 0, 255, 0, 255 ) ) );
		final AdditiveColoringModel< Object > coloringModel = new AdditiveColoringModel<>( colorings );

		final ARGBType color = new ARGBType();
		coloringModel.convert( new Object(), color );

		assertEquals( ARGBType.rgba( 255, 255, 255, 255 ), color.get() );
	}

	@Test
	void ignoresTransparentColors()
	{
		final ArrayList< ColoringModel< Object > > colorings = new ArrayList<>();
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 255, 0, 255, 255 ) ) );
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 0, 255, 0, 0 ) ) );
		final AdditiveColoringModel< Object > coloringModel = new AdditiveColoringModel<>( colorings );

		final ARGBType color = new ARGBType();
		coloringModel.convert( new Object(), color );

		assertEquals( ARGBType.rgba( 255, 0, 255, 255 ), color.get() );
	}

	@Test
	void flattensAdditiveColoringModels()
	{
		final ArrayList< ColoringModel< Object > > firstTwo = new ArrayList<>();
		firstTwo.add( new ConstantColoringModel( ARGBType.rgba( 255, 0, 0, 255 ) ) );
		firstTwo.add( new ConstantColoringModel( ARGBType.rgba( 0, 255, 0, 255 ) ) );
		final AdditiveColoringModel< Object > firstTwoColorings = new AdditiveColoringModel<>( firstTwo );

		final ArrayList< ColoringModel< Object > > colorings = new ArrayList<>();
		colorings.add( firstTwoColorings );
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 0, 0, 255, 255 ) ) );
		final AdditiveColoringModel< Object > coloringModel = new AdditiveColoringModel<>( colorings );

		assertEquals( 3, coloringModel.getColoringModels().size() );
	}

	@Test
	void skipsDisabledColoringModels()
	{
		final ArrayList< ColoringModel< Object > > colorings = new ArrayList<>();
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 255, 0, 255, 255 ) ) );
		colorings.add( new ConstantColoringModel( ARGBType.rgba( 0, 255, 0, 255 ) ) );
		final AdditiveColoringModel< Object > coloringModel = new AdditiveColoringModel<>( colorings );
		coloringModel.setEnabled( coloringModel.getEntries().get( 1 ), false );

		final ARGBType color = new ARGBType();
		coloringModel.convert( new Object(), color );

		assertEquals( ARGBType.rgba( 255, 0, 255, 255 ), color.get() );
	}

	private static class ConstantColoringModel extends AbstractColoringModel< Object >
	{
		private final int argb;

		public ConstantColoringModel( int argb )
		{
			this.argb = argb;
		}

		@Override
		public void convert( Object input, ARGBType output )
		{
			output.set( argb );
		}
	}
}
