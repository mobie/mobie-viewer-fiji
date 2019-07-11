package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;

public class OpenPlatyBrowserHttp
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.2.1",
				"http://10.11.4.195:8000",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/" );

//		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels-new-uint16" );

	}
}
