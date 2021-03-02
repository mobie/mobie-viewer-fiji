package tests;

import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import net.imagej.ImageJ;

import java.util.List;

public class TestPlatyBrowserCells3DView
{
//	@Test
	public void viewSmallAndLargeCellIn3D( )
	{
		new ImageJ().ui().showUI();

		final MoBIE mainFrame = new MoBIE(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		final SourcesDisplayManager sourcesDisplayManager = mainFrame.getSourcesDisplayManager();

		sourcesDisplayManager.show( "sbem-6dpf-1-whole-segmented-cells-labels" );
		final SegmentsTableBdvAnd3dViews views = sourcesDisplayManager.getSourceNameToLabelViews().values().iterator().next();

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
