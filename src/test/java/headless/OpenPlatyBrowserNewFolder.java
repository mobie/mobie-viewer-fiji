package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserNewFolder
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.2.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data/",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data/" );

		final PlatyBrowserSourcesPanel sourcesPanel = platyBrowser.getSourcesPanel();

		//sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells-labels" );

	}
}
