package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

import java.io.File;

public class OpenPlatyBrowserNucleiCells
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser mainFrame = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels" );
		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-nuclei-labels" );

	}
}
