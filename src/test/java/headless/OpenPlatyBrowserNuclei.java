package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserNuclei
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser mainFrame = new PlatyBrowser(
				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/" );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-nuclei-labels" );

	}
}
