import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.PlatyBrowser;
import ij.ImageJ;
import net.imglib2.RealPoint;

public class TestParapodiumDisplay
{
	public static void main( String[] args )
	{

		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-muscles-parapod-fib-aligned" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-raw-parapod-fib-aligned" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-parapod-fib-labels-aligned" );

		BdvUtils.centerBdvViewToPosition(
				platyBrowser.getBdv(),
				new double[]{164,91,142},
				10 );

		platyBrowser.getMainFrame().getActionPanel().showObjectIn3D( new RealPoint( 164, 91, 142 ) );

	}
}
