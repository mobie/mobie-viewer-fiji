package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserGitTablesS3Images
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"test_n5/0.6.5",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data" );
	}
}
