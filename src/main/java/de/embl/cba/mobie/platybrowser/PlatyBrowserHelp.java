package de.embl.cba.mobie.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.tables.Help;

public class PlatyBrowserHelp
{
	public static final String MOBIE = "MoBIE";
	public static final String BIG_DATA_VIEWER = "BigDataViewer";
	public static final String PLATY_BROWSER = "PlatyBrowser";
	public static final String SEGMENTATIONS = "Segmentations";

	public static void showHelp( String selectedItem )
	{
		switch ( selectedItem )
		{
			case MOBIE:
				// TODO
				break;
			case PLATY_BROWSER:
				showPlatyHelp();
				break;
			case BIG_DATA_VIEWER:
				showBdvHelp();
				break;
			case SEGMENTATIONS:
				Help.showSegmentationImageHelp();
		}
	}

	public static void showPlatyHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, MoBIEViewer.class.getResource( "/PlatyHelp.html" ) );
		helpDialog.setVisible( true );
	}

	public static void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, MoBIEViewer.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
