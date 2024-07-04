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

import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;

public class SwingHelper
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int COMBOBOX_WIDTH = 270;
	public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );

	public static JLabel getJLabel( String text )
	{
		return getJLabel( text, 170, 10);
	}

	public static JLabel getJLabel( String text, int width, int height )
	{
		final JLabel comp = new JLabel( text );
		comp.setPreferredSize( new Dimension( width,height ) );
		alignJLabel( comp );
		return comp;
	}

	public static void alignJLabel( JLabel label ) {
		label.setHorizontalAlignment( SwingConstants.LEFT );
		label.setHorizontalTextPosition( SwingConstants.LEFT );
		label.setAlignmentX( Component.LEFT_ALIGNMENT );
	}

	public static JButton createButton( String buttonLabel )
	{
		return createButton( buttonLabel, BUTTON_DIMENSION );
	}

	public static JButton createButton( String buttonLabel, Dimension dimension )
	{
		final JButton button = new JButton( buttonLabel );
		button.setPreferredSize( dimension );
		return button;
	}

	public static void setComboBoxDimensions( JComboBox< String > comboBox, String prototypeDisplayValue )
	{
		comboBox.setPrototypeDisplayValue( prototypeDisplayValue );
		comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
		comboBox.setMaximumSize( new Dimension( Integer.MAX_VALUE, 20 ) );
	}

	public static String selectionDialog( String[] choices, String objectName ) {
		final GenericDialog gd = new GenericDialog("Choose " + objectName );
		gd.addChoice( objectName, choices, choices[0] );
		gd.showDialog();
		if (gd.wasCanceled()) {
			return null;
		} else {
			return gd.getNextChoice();
		}
	}

	public static JPanel horizontalLayoutPanel()
	{
		JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		panel.setBorder( BorderFactory.createEmptyBorder(2, 2, 2, 2) );
		return panel;
	}

}
