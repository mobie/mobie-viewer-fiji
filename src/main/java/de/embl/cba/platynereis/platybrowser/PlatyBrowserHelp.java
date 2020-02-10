package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.Help;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PlatyBrowserHelp
{

	public static final String BIG_DATA_VIEWER = "bigdataviewer";
	public static final String PLATY_BROWSER = "platybrowser";
	public static final String SEGMENTATION_IMAGE = "segmentation image";

	public static void showHelp( String selectedItem )
	{
		switch ( selectedItem )
		{
			case PLATY_BROWSER:
				showPlatyHelp();
				break;
			case BIG_DATA_VIEWER:
				showBdvHelp();
				break;
			case SEGMENTATION_IMAGE:
				Help.showSegmentationImageHelp();
		}
	}

	public static void showPlatyHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, PlatyBrowser.class.getResource( "/PlatyHelp.html" ) );
		helpDialog.setVisible( true );
	}

	public static void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, PlatyBrowser.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}



}
