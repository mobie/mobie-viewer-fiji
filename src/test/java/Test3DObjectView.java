import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.PlatyBrowser;
import de.embl.cba.platynereis.objects.ObjectViewer3D;
import ij.ImageJ;
import net.imglib2.RealPoint;

public class Test3DObjectView
{
	public static void main( String[] args )
	{

		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-parapod-fib-labels-aligned" );

//		BdvUtils.zoomToPosition(
//				platyBrowser.getBdv(),
//				new double[]{164,91,142},
//				10 );


		new ObjectViewer3D().showSelectedObjectIn3D( platyBrowser.getBdv(), new RealPoint( 164, 91, 142 ), 0.1 );

	}
}
