package tests;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.view.AdditionalViewsLoader;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;

public class TestRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		new TestRemoteZebrafish().testSmallGridView();
		new TestRemoteZebrafish().testTransformedGridView();
	}

	@Test
	public void testSmallGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		moBIE.getViewManager().show( "small-grid-view" );

		// select some image segments
		final AnnotationDisplay display = moBIE.getViewManager().getAnnotatedRegionDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ), this );
		display.selectionModel.setSelected( display.tableRows.get( 1 ), true );
		display.selectionModel.focus( display.tableRows.get( 1 ), this );

		// show in 3D
		new Thread( () -> { (( SegmentationDisplay ) display).segmentsVolumeViewer.showSegments( true ); } ).start();
		//(( SegmentationDisplay ) display).segmentsVolumeViewer.showSegments( true );
	}

	@Test
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
		final AnnotationDisplay display = moBIE.getViewManager().getAnnotatedRegionDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ), this );
	}
}
