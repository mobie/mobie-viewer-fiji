package develop;

import de.embl.cba.mobie.ui.MoBIE;

public class DevelopAnnotationCorrectionPlatynereis
{
	public static void main( String[] args )
	{
		final MoBIE viewer = new MoBIE( "https://github.com/vzinche/platybrowser-backend" );
		viewer.getSourcesDisplayManager().addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells" );

		// TODO: develop API get the views of a source: getViews( String sourceName )
	}
}
