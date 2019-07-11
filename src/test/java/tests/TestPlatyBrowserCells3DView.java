package tests;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import net.imagej.ImageJ;

public class TestPlatyBrowserCells3DView
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser mainFrame = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr", "https://git.embl.de/tischer/platy-browser-tables/raw/master/" );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "em-segmented-cells-labels" );
		final SegmentsTableBdvAnd3dViews views = sourcesPanel.getViews();

		final SelectionModel< TableRowImageSegment > selectionModel = views.getSelectionModel();



	}
}
