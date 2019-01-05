import de.embl.cba.platynereis.PlatyBrowser;
import ij.ImageJ;

public class Test3DObjectView
{
	public static void main( String[] args )
	{

		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		platyBrowser.getMainUI().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-labels" );

//		BdvUtils.zoomToPosition(
//				platyBrowser.getBdv(),
//				new double[]{164,91,142},
//				10 );


//		new ObjectViewer3D( source ).showSelectedObjectIn3D( platyBrowser.getBdv(), new RealPoint( 164, 91, 142 ), 0.1 );

	}
}
