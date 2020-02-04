package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;

public class OpenPlatyBrowserRemoteV1
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"1.0.0",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5" );
	}
}
