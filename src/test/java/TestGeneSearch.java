import de.embl.cba.platynereis.PlatyBrowser;
import ij.ImageJ;

import java.util.ArrayList;

public class TestGeneSearch
{
	public static void main( String[] args )
	{
		new ImageJ();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		final ArrayList< Double > geneSearchRadii = platyBrowser.getMainUI().getActionPanel().getGeneSearchRadii();

		platyBrowser.getMainUI().getActionPanel().searchGenes(
				new double[]{126,132,142},
				geneSearchRadii.get( 0 ) );

	}
}
