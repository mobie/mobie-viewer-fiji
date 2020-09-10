package de.embl.cba.mobie.ui.viewer;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Help;

public class MoBIEInfo
{
	public static final String MOBIE_VIEWER = "MoBIE Viewer";
	public static final String MOBIE_FRAMEWORK = "MoBIE Framework";
	public static final String BIG_DATA_VIEWER = "BigDataViewer";
	public static final String REPOSITORY = "Repository";
	public static final String SEGMENTATIONS = "Segmentations Browsing";
	private final String projectLocation;

	public MoBIEInfo( String projectLocation )
	{
		this.projectLocation = projectLocation;
	}

	public static String[] getInfoChoices()
	{
		return new String[]{
				MOBIE_VIEWER,
				MOBIE_FRAMEWORK,
				REPOSITORY,
				BIG_DATA_VIEWER,
				SEGMENTATIONS };
	}

	public void showInfo( String selectedItem )
	{
		switch ( selectedItem )
		{
			case MOBIE_VIEWER:
				FileAndUrlUtils.openURI( "https://github.com/mobie/mobie-viewer-fiji/blob/master/README.md#mobie-fiji-viewer" );
				break;
			case MOBIE_FRAMEWORK:
				FileAndUrlUtils.openURI( "https://github.com/mobie/mobie#mobie" );
				break;
			case REPOSITORY:
				final String uri = FileAndUrlUtils.combinePath( projectLocation, "blob/master/README.md" );
				FileAndUrlUtils.openURI( uri );
				break;
			case BIG_DATA_VIEWER:
				showBdvHelp();
				break;
			case SEGMENTATIONS:
				Help.showSegmentationImageHelp();
		}
	}

	public void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, MoBIEViewer.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
