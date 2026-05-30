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

public class AdditiveColoringModelDialog extends JFrame
{
	private final AdditiveColoringModel< ? > coloringModel;

	public AdditiveColoringModelDialog( AdditiveColoringModel< ? > coloringModel )
	{
		this.coloringModel = coloringModel;
		setTitle( "Coloring Stack" );
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				360, 480 );
		refresh();
		setVisible( true );
	}

	public void refresh()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		for ( AdditiveColoringModel.Entry< ? > entry : coloringModel.getEntries() )
			panel.add( createEntryPanel( entry ) );

		setContentPane( new JScrollPane( panel ) );
		pack();
		revalidate();
		repaint();
	}

	private JPanel createEntryPanel( AdditiveColoringModel.Entry< ? > entry )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		final JPanel header = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JCheckBox enabledCheckBox = new JCheckBox();
		enabledCheckBox.setSelected( entry.isEnabled() );
		enabledCheckBox.addActionListener( e -> setEntryEnabled( entry, enabledCheckBox.isSelected() ) );
		header.add( enabledCheckBox );
		header.add( new JLabel( entry.getName() ) );
		panel.add( header );

		final ColoringModel< ? > entryColoringModel = entry.getColoringModel();
		if ( entryColoringModel instanceof NumericAnnotationColoringModel )
		{
			final NumericAnnotationColoringModel< ? > numericColoringModel = ( NumericAnnotationColoringModel< ? > ) entryColoringModel;
			panel.add( new NumericAnnotationColoringModelContrastPanel( numericColoringModel ) );
		}
		else
		{
			final JLabel label = new JLabel( "No contrast controls available." );
			label.setBorder( BorderFactory.createEmptyBorder( 0, 24, 0, 0 ) );
			panel.add( label );
		}

		return panel;
	}

	@SuppressWarnings( "unchecked" )
	private < T > void setEntryEnabled( AdditiveColoringModel.Entry< T > entry, boolean enabled )
	{
		( ( AdditiveColoringModel< T > ) coloringModel ).setEnabled( entry, enabled );
	}
}
