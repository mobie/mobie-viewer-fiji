import de.embl.cba.platynereis.platybrowser.PlatyBrowserMainFrame;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;

import java.io.File;
import java.util.ArrayList;

public class OpenPlatyBrowser
{
	public static void main( String[] args )
	{
		File dataFolder = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

		final PlatyBrowserMainFrame mainFrame = new PlatyBrowserMainFrame( dataFolder );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		final ArrayList< String > sourceNames = sourcesPanel.getSourceNames();
		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels" );
		sourcesPanel.addSourceToPanelAndViewer( "em-raw-parapod-fib-affine_g" );
	}
}
