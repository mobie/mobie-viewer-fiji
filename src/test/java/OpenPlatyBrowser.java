import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

import java.io.File;

public class OpenPlatyBrowser
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		File dataFolder = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		final PlatyBrowser mainFrame = new PlatyBrowser( dataFolder );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels-new-uint16" );

		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-new-nuclei-uint16-labels" );

	}
}
