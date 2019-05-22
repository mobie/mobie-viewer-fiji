package headless;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

import java.io.File;

public class OpenPlatyBrowserHttp
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser( "http://10.11.4.195:8000" );

//		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

//		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels-new-uint16" );

	}
}
