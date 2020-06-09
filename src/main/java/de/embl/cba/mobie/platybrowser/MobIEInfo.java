package de.embl.cba.mobie.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Help;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MobIEInfo
{
	public static final String MOBIE = "MoBIE";
	public static final String BIG_DATA_VIEWER = "BigDataViewer";
	public static final String REPOSITORY = "Repository";
	public static final String SEGMENTATIONS = "Segmentations Browsing";
	private final String projectLocation;

	public MobIEInfo( String projectLocation )
	{
		this.projectLocation = projectLocation;
	}

	public void showInfo( String selectedItem )
	{
		switch ( selectedItem )
		{
			case MOBIE:
				FileAndUrlUtils.openURI( "https://github.com/mobie-org/mobie-viewer-fiji/blob/master/README.md" );
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
