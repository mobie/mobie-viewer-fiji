package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;

public class OpenPlatyBrowserHttp
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.6.0",
				"http://cbb-bigdata01.embl.de",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data" );

//		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels-new-uint16" );

	}
}
