package headless;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import de.embl.cba.platynereis.platybrowser.SourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserLocalImageGitTable
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"0.6.0",
				null, "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data" );

		final SourcesPanel sourcesPanel = moBIEViewer.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells-labels" );
	}
}
