package de.embl.cba.mobie.ui;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Help;
import ij.IJ;

public class MoBIEInfo
{
	public static final String MOBIE_VIEWER = "MoBIE Viewer";
	public static final String MOBIE_FRAMEWORK = "MoBIE Framework";
	public static final String BIG_DATA_VIEWER = "BigDataViewer";
	public static final String REPOSITORY = "Datasets Repository";
	public static final String PUBLICATION = "Datasets Publication";
	public static final String SEGMENTATIONS = "Segmentations Browsing";
	private final String projectLocation;
	private final String publicationURL;

	public MoBIEInfo( String projectLocation, String publicationURL )
	{
		this.projectLocation = projectLocation;
		this.publicationURL = publicationURL;
	}

	public String[] getInfoChoices()
	{
		return new String[]{
				REPOSITORY,
				PUBLICATION,
				MOBIE_VIEWER,
				MOBIE_FRAMEWORK,
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
				FileAndUrlUtils.openURI( FileAndUrlUtils.combinePath( projectLocation, "blob/master/README.md" ) );
				break;
			case PUBLICATION:
				if ( publicationURL == null )
				{
					IJ.showMessage( "There is no publication registered with this project.");
					return;
				}
				else
				{
					FileAndUrlUtils.openURI( publicationURL );
				}
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
		HelpDialog helpDialog = new HelpDialog( null, ProjectManager.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
