package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenPlatybrowserTest
{
	public static void main( final String... args ) throws IOException
	{
		new OpenPlatybrowserTest().testFigure1c();
	}

	@Test
	public void testCellPublicationFigure2C() throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets" );
		moBIE.getViewManager().show( "Figure 2C: Muscle segmentation" );
	}

	@Test
	public void testFigure1c() throws IOException
	{
		// Init
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Open
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().view( "Figure1c" ) );

		// The second display should be of the segmented cells.
		final List< SourceDisplay > displays = moBIE.getViewManager().getCurrentSourceDisplays();
		final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) displays.get( 1 );
		System.out.println( segmentationDisplay.getName() );

		// Both scatter plot and segment volume rendering
		// should be enabled.
		final boolean scatterPlotViewerVisible = segmentationDisplay.scatterPlotViewer.isVisible();
		final boolean volumeViewerShowSegments = segmentationDisplay.segmentsVolumeViewer.isShowSegments();

		assertTrue( scatterPlotViewerVisible );
		assertTrue( volumeViewerShowSegments );
	}
}
