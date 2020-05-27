package headless;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.mobie.viewer.SourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserLocalImageLocalTable
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data" );

		final SourcesPanel sourcesPanel = moBIEViewer.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-nuclei-labels" );
//		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-muscles" );
	}
}
