package headless;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import mpicbg.spim.data.SpimData;
import net.imagej.ImageJ;

public class OpenPlatyBrowserHttpBigDataServer
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.6.0",
				"http://cbb-bigdata01.embl.de",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data" );

		final PlatyBrowserSourcesPanel sourcesPanel = platyBrowser.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells-labels" );
	}
}
