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
package org.embl.mobie3.viewer.color;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.tables.color.ColoringListener;

import javax.swing.*;
import java.awt.*;

public class NumericColoringModelDialog extends JFrame implements ColoringListener
{
	public static Point dialogLocation;

	public NumericColoringModelDialog(
			final String coloringFeature,
			final NumericAnnotationColoringModel< ? > coloringModel )
	{
		// configure UI range relative to current
		// contrast limits; the same logic is
		// used for setting the contrast limits of the
		// images, see UserInterfaceHelper.showBrightnessDialog()
		final double absCurrentRange = Math.abs( coloringModel.getMax() - coloringModel.getMin() );
		final double rangeFactor = 1.0; // could be adapted
		final double rangeMin = coloringModel.getMin() - rangeFactor * absCurrentRange;
		final double rangeMax = coloringModel.getMax() + rangeFactor * absCurrentRange;

		final BoundedValueDouble min = new BoundedValueDouble(
				rangeMin,
				rangeMax,
				coloringModel.getMin() );
		final BoundedValueDouble max = new BoundedValueDouble(
				rangeMin,
				rangeMax,
				coloringModel.getMax() );

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		double spinnerStepSize = ( rangeMax - rangeMin ) / 100.0;

		final SliderPanelDouble minSlider = new SliderPanelDouble(
				"Min", min, spinnerStepSize );
		minSlider.setNumColummns( 7 );
		minSlider.setDecimalFormat( "####E0" );

		final SliderPanelDouble maxSlider = new SliderPanelDouble(
				"Max", max, spinnerStepSize );
		maxSlider.setNumColummns( 7 );
		maxSlider.setDecimalFormat( "####E0" );

		class UpdateListener implements BoundedValueDouble.UpdateListener
		{
			@Override
			public void update()
			{
				coloringModel.setMin( min.getCurrentValue() );
				coloringModel.setMax( max.getCurrentValue() );
				minSlider.update();
				maxSlider.update();
			}
		}

		final UpdateListener updateListener = new UpdateListener();

		min.setUpdateListener( updateListener );
		max.setUpdateListener( updateListener );

		panel.add( minSlider );
		panel.add( maxSlider );

		final JFrame frame = new JFrame( coloringFeature );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setContentPane( panel );
		frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.pack();
		frame.setVisible( true );
		frame.setResizable( false );
		if ( dialogLocation != null )
			frame.setLocation( dialogLocation );
	}

	public void close()
	{
		dialogLocation = getLocation();
		dispose();
	}


	@Override
	public void coloringChanged()
	{

	}
}
