package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;

public class OpenPlatyBrowserLocal
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.6.5",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data" );
	}
}
