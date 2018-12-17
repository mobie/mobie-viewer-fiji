import de.embl.cba.platynereis.PlatyBrowser;
import ij.ImageJ;

public class TestParapodium
{

	public static void main( String[] args )
	{
		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-raw-parapod-fib.xml-aligned" );


	}
}
