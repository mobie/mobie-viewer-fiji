package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;

public class OpenPlatyBrowserRemote
{
	public static void main( String[] args )
	{
		// new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.6.6",
				"https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data",
				"https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data" );
	}
}
