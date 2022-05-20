package org.embl.mobie.viewer;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.view.View;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MoBIEViewerTest
{
	static {
		LegacyInjector.preinit();
	}

	private MoBIE moBIE;

	@BeforeAll
	public static void initIJ() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
	}

	@Test
	public void testCLEMFigure2a() throws IOException
	{
		// Open
		moBIE = new MoBIE( "https://github.com/mobie/clem-example-project", MoBIESettings.settings().view( "Figure2a" ) );

		// Test
		moBIE.sourceNameToSourceAndConverter().keySet().stream().forEach( s -> System.out.println( s ) );
		final SourceAndConverter< ? > sourceAndConverter = moBIE.sourceNameToSourceAndConverter().get( "fluorescence-annotations" );
		final LabelSource< ? > labelSource = SourceHelper.getLabelSource( sourceAndConverter );
		final boolean showAsBoundaries = labelSource.isShowAsBoundaries();
		assertTrue( showAsBoundaries );
	}

	@Test
	public void testCovidIFDefault() throws IOException
	{
		moBIE = new MoBIE( "https://github.com/mobie/covid-if-project" );
	}

	@Test
	public void testCellPublicationFigure2C() throws IOException
	{
		moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets" );
		moBIE.getViewManager().show( "Figure 2C: Muscle segmentation" );
	}

	//@Test
	public void testSubmissionFigure1c() throws IOException
	{
		// Open
		moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().view( "Figure1c" ) );

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

	@Test
	public void testAllOnS3() throws IOException
	{
		// This is special as it does not go via github but
		// all information is on S3
		moBIE = new MoBIE("https://s3.embl.de/plankton-fibsem", MoBIESettings.settings());

		// Check all views
		final Map< String, View > views = moBIE.getViews();
		for ( View view : views.values() )
		{
			moBIE.getViewManager().show( view );
		}
	}

	@Test
	public void testZebraFishSmallGridView() throws IOException
	{
		// Open
		moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).view( "small-grid-view" ) );

		// Select some image segments
		final AnnotationDisplay display = moBIE.getViewManager().getAnnotatedRegionDisplays().iterator().next();
		display.selectionModel.setSelected( display.tableRows.get( 0 ), true );
		display.selectionModel.focus( display.tableRows.get( 0 ), this );
		display.selectionModel.setSelected( display.tableRows.get( 1 ), true );
		display.selectionModel.focus( display.tableRows.get( 1 ), this );

		// Show in 3D
		(( SegmentationDisplay ) display).segmentsVolumeViewer.showSegments( true );
	}

	@AfterEach
	public void closeMoBIE()
	{
		try
		{
			moBIE.close();
		}
		catch ( Exception e )
		{
			// MoBIE sometimes is still lazy-loading something
			// and thus will throw errors when being closed.
		}
	}
}
