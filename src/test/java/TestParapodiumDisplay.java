import de.embl.cba.platynereis.PlatyBrowser;

public class TestParapodiumDisplay
{
	public static void main( String[] args )
	{
		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-raw-rachel-fib-aligned" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-muscles-ariadne" );
	}
}
