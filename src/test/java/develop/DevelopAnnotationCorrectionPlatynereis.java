package develop;

import de.embl.cba.mobie.ui.ProjectManager;

public class DevelopAnnotationCorrectionPlatynereis
{
	public static void main( String[] args )
	{
		final ProjectManager viewer = new ProjectManager( "https://github.com/vzinche/platybrowser-backend" );
		viewer.getSourcesManager().addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells" );

		// TODO: develop API get the views of a source: getViews( String sourceName )
	}
}
