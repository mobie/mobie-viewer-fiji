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

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.awt.*;

public class NumericAnnotationColoringModelContrastPanel extends JPanel
{
	public NumericAnnotationColoringModelContrastPanel( final NumericAnnotationColoringModel< ? > coloringModel )
	{
		setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );

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

		// place sliders in a vertical panel on the left and the color button (if present)
		// to the right so the color control doesn't take extra vertical space
		final JPanel slidersPanel = new JPanel();
		slidersPanel.setLayout( new BoxLayout( slidersPanel, BoxLayout.PAGE_AXIS ) );
		slidersPanel.add( minSlider );
		slidersPanel.add( maxSlider );

		final JPanel horizontal = new JPanel( new BorderLayout() );
		horizontal.add( slidersPanel, BorderLayout.CENTER );

		if ( coloringModel.isSingleColorLut() )
		{
			// createSingleColorPanel returns a small panel containing the Color button
			horizontal.add( createSingleColorPanel( coloringModel ), BorderLayout.EAST );
		}

		add( horizontal );
	}

	private JPanel createSingleColorPanel( NumericAnnotationColoringModel< ? > coloringModel )
	{
		final JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JButton button = new JButton( "Color" );
		button.setToolTipText( "Change single color LUT color" );

		final ARGBType currentColor = coloringModel.getSingleColor();
		if ( currentColor != null )
			button.setBackground( ColorHelper.getColor( currentColor ) );

		button.addActionListener( e ->
		{
			final Color color = JColorChooser.showDialog(
					panel,
					"Choose single color LUT color",
					button.getBackground() );

			if ( color == null )
				return;

			button.setBackground( color );
			coloringModel.setSingleColor( ColorHelper.getARGBType( color ) );
		} );

		// The button label "Color" is self-explanatory; no extra label required.
		panel.add( button );

		return panel;
	}
}
