package org.embl.mobie.viewer.ui;

import org.embl.mobie.viewer.MoBIE;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;

public class SwingHelper
{
	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int COMBOBOX_WIDTH = 270;
	public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );

	public static JPanel horizontalLayoutPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
		panel.add( Box.createHorizontalGlue() );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );

		return panel;
	}

	public static JLabel getJLabel( String text )
	{
		return getJLabel( text, 170, 10);
	}

	public static JLabel getJLabel( String text, int width, int height )
	{
		final JLabel comp = new JLabel( text );
		comp.setPreferredSize( new Dimension( width,height ) );
		comp.setHorizontalAlignment( SwingConstants.LEFT );
		comp.setHorizontalTextPosition( SwingConstants.LEFT );
		comp.setAlignmentX( Component.LEFT_ALIGNMENT );
		return comp;
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

	public static void setComboBoxDimensions( JComboBox< String > comboBox )
	{
		comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE );
		comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
		comboBox.setMaximumSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
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

}
