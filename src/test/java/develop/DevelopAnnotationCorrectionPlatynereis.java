package develop;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;

public class DevelopAnnotationCorrectionPlatynereis
{
	public static void main( String[] args )
	{
		final MoBIEViewer viewer = new MoBIEViewer( "https://github.com/vzinche/platybrowser-backend" );
		viewer.getSourcesPanel().addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells" );

		// TODO: develop API get the views of a source: getViews( String sourceName )
	}
}
