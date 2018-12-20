import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.PlatyBrowser;
import ij.ImageJ;

public class TestPlatyBrowser
{
	public static void main( String[] args )
	{

		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		Bdv bdv = platyBrowser.getBdv();

//		BdvUtils.centerBdvViewToPosition(
//				bdv,
//				new double[]{222,157,57},
//				10 );

	}
}
