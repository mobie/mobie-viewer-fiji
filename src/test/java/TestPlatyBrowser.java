import de.embl.cba.platynereis.PlatyBrowser;

public class TestPlatyBrowser
{
	public static void main( String[] args )
	{
		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-gut-labels" );

	}
}
