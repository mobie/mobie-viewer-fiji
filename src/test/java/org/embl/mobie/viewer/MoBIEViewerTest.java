/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.source.AnnotationSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.view.View;
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
	public static void initIJ()
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
		final AnnotationSource< ? > annotationSource = SourceHelper.getLabelSource( sourceAndConverter );
		final boolean showAsBoundaries = annotationSource.isShowAsBoundaries();
		assertTrue( showAsBoundaries );
	}

	@Test
	public void testCovidIFDefault() throws IOException
	{
		moBIE = new MoBIE( "https://github.com/mobie/covid-if-project" );
	}

	//@Test
	// This loads the whole plate, thus probably too much
	// to be run as an actual test.
	public void testCovidIFFullPlate() throws IOException
	{
		moBIE = new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().view( "full grid" ) );
	}

	@Test
	public void testCellPublicationFigure2C() throws IOException
	{
		moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().view( "Figure 2C: Muscle segmentation" ) );
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
		(( SegmentationDisplay ) display).segmentsVolumeViewer.showSegments( true, true );
	}

	//@Test
	// This test is quite memory intensive and thus
	// it probably is best if it runs last.
	// TODO: this crashes in the github actions CI
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
}
