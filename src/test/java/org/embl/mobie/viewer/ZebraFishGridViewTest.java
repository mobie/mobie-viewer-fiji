package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.view.AdditionalViewsLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZebraFishGridViewTest
{
	public static void main( String[] args ) throws IOException
	{
		new ZebraFishGridViewTest().testSmallGridView();
	}

	@Test
	public void testSmallGridView() throws IOException
	{
		// Init
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		// Open
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).view( "small-grid-view" ) );

		// Select some image segments
		final AnnotationDisplay display = moBIE.getViewManager().getAnnotatedRegionDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ), this );
		display.selectionModel.setSelected( display.tableRows.get( 1 ), true );
		display.selectionModel.focus( display.tableRows.get( 1 ), this );

		// Show in 3D
		(( SegmentationDisplay ) display).segmentsVolumeViewer.showSegments( true );
	}
}
