package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PlatyHelp
{

	public static final String KEYBOARD_SHORTCUTS_IN_BIG_DATA_VIEWER_GENERAL = "General Keyboard Shortcuts in BigDataViewer";
	public static final String KEYBOARD_SHORTCUTS_IN_BIG_DATA_VIEWER_PLATY_BROWSER_SPECIFIC = "PlatyBrowser Specific Keyboard Shortcuts in BigDataViewer";
	public static final String PLATYBROWSER_GENERAL = "PlatyBrowser General";

	public static void showHelp( String selectedItem )
	{
		switch ( selectedItem )
		{
			case KEYBOARD_SHORTCUTS_IN_BIG_DATA_VIEWER_PLATY_BROWSER_SPECIFIC:
				showPlatyHelp();
				break;
			case KEYBOARD_SHORTCUTS_IN_BIG_DATA_VIEWER_GENERAL:
				showBdvHelp();
				break;
		}
	}

	public static void showPlatyHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, PlatyBrowser.class.getResource( "/PlatyHelp.html" ) );
		helpDialog.setVisible( true );

		AbstractAction help = new AbstractAction()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				helpDialog.setVisible( ! helpDialog.isVisible() );
			}
		};

//		this.getRootPane().getActionMap().put("help", help );
	}

	public static void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, PlatyBrowser.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );

		AbstractAction help = new AbstractAction()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				helpDialog.setVisible( ! helpDialog.isVisible() );
			}
		};

//		this.getRootPane().getActionMap().put("help", help );
	}



}
