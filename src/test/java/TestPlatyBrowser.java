import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.PlatyBrowser;

public class TestPlatyBrowser
{
	public static void main( String[] args )
	{
		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-gut-labels" );

		Bdv bdv = platyBrowser.getBdv();

//		BdvUtils.centerBdvViewToPosition(
//				bdv,
//				new double[]{222,157,57},
//				10 );

	}
}
