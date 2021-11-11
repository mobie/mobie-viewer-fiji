package tests;

import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.source.ImageDataFormat;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViewsLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class TestRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		new TestRemoteZebrafish().testSmallGridView();
		//new TestRemoteZebrafish().testTransformedGridView();
	}

	@Test
	public void testSmallGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		moBIE.getViewManager().show( "small-grid-view" );

		// select some image segments
		final SegmentationSourceDisplay display = moBIE.getViewManager().getSegmentationDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ) );
		display.selectionModel.setSelected( display.tableRows.get( 1 ), true );
		display.selectionModel.focus( display.tableRows.get( 1 ) );

		// show in 3D
		display.segmentsVolumeViewer.showSegments( true );
	}

	//@Test
	public void testTransformedGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		// TODO: does not show the sources
		final AdditionalViewsLoader viewsLoader = new AdditionalViewsLoader( moBIE );
		viewsLoader.loadViews( "https://raw.githubusercontent.com/mobie/zebrafish-lm-datasets/main/data/membrane/misc/views/test_views.json" );
		moBIE.getViewManager().show( "test-transformed-grid" );

		// select some image segments
		final SegmentationSourceDisplay display = moBIE.getViewManager().getSegmentationDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ) );
	}
}
