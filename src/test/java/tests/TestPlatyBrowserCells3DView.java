package tests;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import net.imagej.ImageJ;
import org.junit.Test;

import java.util.List;

public class TestPlatyBrowserCells3DView
{
	@Test
	public void viewSmallAndLargeCellIn3D( )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser mainFrame = new PlatyBrowser(
				"0.2.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		final PlatyBrowserSourcesPanel sourcesPanel = mainFrame.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-cells-labels" );
		final SegmentsTableBdvAnd3dViews views = sourcesPanel.getViews();

		final SelectionModel< TableRowImageSegment > selectionModel = views.getSelectionModel();
		final List< TableRowImageSegment > tableRowImageSegments = views.getTableRowImageSegments();

		selectionModel.setSelected( tableRowImageSegments.get( 11057 ), true ); // small cell
		selectionModel.setSelected( tableRowImageSegments.get( 5703 ), true ); // neuropil
	}


	public static void main( String[] args )
	{
		new TestPlatyBrowserCells3DView().viewSmallAndLargeCellIn3D( );
	}
}
