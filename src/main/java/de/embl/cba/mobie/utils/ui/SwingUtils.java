package de.embl.cba.mobie.utils.ui;

import de.embl.cba.mobie.ui.ProjectManager;

import javax.swing.*;
import java.awt.*;

public class SwingUtils
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

	public static void resetSwingLookAndFeel() {
		try {
			UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
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

	public static JButton getButton( String buttonLabel )
	{
		return getButton( buttonLabel, BUTTON_DIMENSION );
	}

	public static JButton getButton( String buttonLabel, Dimension dimension )
	{
		final JButton button = new JButton( buttonLabel );
		button.setPreferredSize( dimension );
		return button;
	}

	public static void setComboBoxDimensions( JComboBox< String > comboBox )
	{
		comboBox.setPrototypeDisplayValue( ProjectManager.PROTOTYPE_DISPLAY_VALUE );
		comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
		comboBox.setMaximumSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
	}

}
