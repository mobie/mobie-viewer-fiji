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

import javax.swing.*;
import java.awt.*;

public class ColoringModelAdjustmentDialog extends JFrame
{
	private final ColoringModel< ? > coloringModel;

	public ColoringModelAdjustmentDialog( ColoringModel< ? > coloringModel )
	{
		this.coloringModel = coloringModel;
		setTitle( "Coloring Adjustment" );
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				360, 480 );
		refresh();

		setVisible( coloringModel );
	}

	public void refresh()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		if ( coloringModel instanceof AdditiveColoringModel )
		{
			final AdditiveColoringModel< ? > additive = ( AdditiveColoringModel< ? > ) coloringModel;
			for ( AdditiveColoringModel.Entry< ? > entry : additive.getEntries() )
				panel.add( createEntryPanel( additive, entry ) );
		}
		else
		{
			panel.add( createSingleModelPanel( coloringModel ) );
		}

		setContentPane( new JScrollPane( panel ) );
		pack();
		revalidate();
		repaint();
		setVisible( coloringModel );
	}

	private void setVisible( ColoringModel< ? > coloringModel )
	{
		// only show if there is at least one coloring model
		// that has adjustments

		if ( coloringModel instanceof CategoricalAnnotationColoringModel )
		{
			setVisible( false );
		}
		else if ( coloringModel instanceof AdditiveColoringModel )
		{
			boolean allCategorical = ( ( AdditiveColoringModel< ? > ) coloringModel ).getColoringModels()
					.stream().allMatch( cm -> cm instanceof CategoricalAnnotationColoringModel );
			setVisible( ! allCategorical );
		}
		else
		{
			setVisible( true );
		}
	}

	private JPanel createEntryPanel( AdditiveColoringModel< ? > additive, AdditiveColoringModel.Entry< ? > entry )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		final JPanel header = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JCheckBox enabledCheckBox = new JCheckBox();
		enabledCheckBox.setSelected( entry.isEnabled() );
		enabledCheckBox.addActionListener( e -> setEntryEnabled( additive, entry, enabledCheckBox.isSelected() ) );
		header.add( enabledCheckBox );
		header.add( new JLabel( entry.getName() ) );
		panel.add( header );

		panel.add( createContrastComponent( entry.getColoringModel() ) );

		return panel;
	}

	private JPanel createSingleModelPanel( ColoringModel< ? > model )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		final JPanel header = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		header.add( new JLabel( ColoringModels.getName( model ) ) );
		panel.add( header );

		panel.add( createContrastComponent( model ) );

		return panel;
	}

	private JComponent createContrastComponent( ColoringModel< ? > model )
	{
		if ( model instanceof NumericAnnotationColoringModel )
		{
			return new NumericAnnotationColoringModelContrastPanel( ( NumericAnnotationColoringModel< ? > ) model );
		}
		final JPanel wrapper = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JLabel label = new JLabel( "No contrast controls available." );
		label.setBorder( BorderFactory.createEmptyBorder( 0, 24, 0, 0 ) );
		wrapper.add( label );
		return wrapper;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void setEntryEnabled( AdditiveColoringModel< ? > additive, AdditiveColoringModel.Entry< ? > entry, boolean enabled )
	{
		( ( AdditiveColoringModel ) additive ).setEnabled( entry, enabled );
	}
}
