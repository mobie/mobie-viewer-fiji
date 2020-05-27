package headless;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.mobie.viewer.SourcesPanel;
import net.imagej.ImageJ;

public class OpenPlatyBrowserHttpBigDataServer
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"http://cbb-bigdata01.embl.de",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data" );

		final SourcesPanel sourcesPanel = moBIEViewer.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells-labels" );
	}
}
