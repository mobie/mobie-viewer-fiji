import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;

import java.io.File;
import java.util.ArrayList;

public class OpenPlatyBrowser
{
	public static void main( String[] args )
	{
		File dataFolder = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		final PlatyBrowser mainFrame = new PlatyBrowser( dataFolder );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		final ArrayList< String > sourceNames = sourcesPanel.getSourceNames();

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels-new-uint16" );

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-new-nuclei-uint16-labels" );

	}
}
