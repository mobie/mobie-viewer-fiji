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
package org.embl.mobie.ui;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class BrightnessUpdateListener implements BoundedValueDouble.UpdateListener
{
	final private SacProvider sacProvider;
 	final private BoundedValueDouble min;
	final private BoundedValueDouble max;
	private final SliderPanelDouble minSlider;
	private final SliderPanelDouble maxSlider;

	public BrightnessUpdateListener( BoundedValueDouble min,
									 BoundedValueDouble max,
									 SliderPanelDouble minSlider,
									 SliderPanelDouble maxSlider,
									 SacProvider sacProvider )
	{
		this.min = min;
		this.max = max;
		this.minSlider = minSlider;
		this.maxSlider = maxSlider;
		this.sacProvider = sacProvider;
	}

	@Override
	public void update()
	{
		final double minCurrentValue = min.getCurrentValue();
		final double maxCurrentValue = max.getCurrentValue();

		// TODO This sort of works, but the spinner step size
		//   would also need to be adapted for this to be really useful
		//   see https://github.com/mobie/mobie-viewer-fiji/issues/825
//		final double log10CurrentRange = Math.log10( Math.abs( maxCurrentValue - minCurrentValue ) );
//
//		if ( log10CurrentRange < 0 )
//		{
//			final int numDigits = ( int ) ( -log10CurrentRange + 1 );
//			String format = "#.";
//			for ( int i = 0; i <= numDigits; i++ )
//				format += "#";
//			minSlider.setDecimalFormat( format );
//			maxSlider.setDecimalFormat( format );
//		}
//		else
//		{
//			final int numDigits = ( int ) ( log10CurrentRange + 1 );
//			String format = "";
//			for ( int i = 0; i <= numDigits; i++ )
//				format += "#";
//			minSlider.setDecimalFormat( format );
//			maxSlider.setDecimalFormat( format );
//		}

		minSlider.update();
		maxSlider.update();

		sacProvider.get()
				.stream()
				.map( sac -> SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ) )
				.forEach( cs -> cs.setDisplayRange( minCurrentValue, maxCurrentValue ) );
	}
}
