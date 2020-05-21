package headless;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserLocalImageLocalTable
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"0.6.0",
				null, "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data" );

		final PlatyBrowserSourcesPanel sourcesPanel = moBIEViewer.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-nuclei-labels" );
//		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-muscles" );
	}
}
