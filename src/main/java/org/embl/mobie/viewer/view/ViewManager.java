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
package org.embl.mobie.viewer.view;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.annotate.AnnotatedMaskAdapter;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.bdv.view.RegionSliceView;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.bdv.view.SegmentationSliceView;
import org.embl.mobie.viewer.bdv.view.SliceViewer;
import org.embl.mobie.viewer.color.SelectionColoringModel;
import org.embl.mobie.viewer.display.*;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.segment.SegmentAdapter;
import org.embl.mobie.viewer.select.MoBIESelectionModel;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.table.TableViewer;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.viewer.transform.SourceTransformer;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.save.ViewSaver;
import org.embl.mobie.viewer.volume.ImageVolumeViewer;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import org.embl.mobie.viewer.volume.UniverseManager;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public class ViewManager
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private List<SourceDisplay> currentSourceDisplays;
	private List<SourceTransformer> currentSourceTransformers;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;

    public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentSourceDisplays = new ArrayList<>();
		currentSourceTransformers = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D, timepoints );
		universeManager = new UniverseManager();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

	public static void initScatterPlotViewer( AnnotationDisplay< ? > display )
	{
		if ( display.tableRows.size() == 0 ) return;

		String[] scatterPlotAxes = display.getScatterPlotAxes();
		display.scatterPlotViewer = new ScatterPlotViewer( display.tableRows, display.selectionModel, display.coloringModel, scatterPlotAxes, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );

		if ( display.showScatterPlot() )
		{
			display.scatterPlotViewer.setShowColumnSelectionUI( false );
			display.scatterPlotViewer.show();
		}
	}

	private static void configureColoringModel( AnnotationDisplay< ? extends TableRow > display )
	{
		if ( display.getColorByColumn() != null )
		{
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( display.tableRows );
			final ColoringModel< TableRowImageSegment > coloringModel;
			String coloringLut = display.getLut();

			if ( display.getValueLimits() != null )
			{
				coloringModel = modelCreator.createColoringModel(display.getColorByColumn(), coloringLut, display.getValueLimits()[0], display.getValueLimits()[1]);
			}
			else
			{
				coloringModel = modelCreator.createColoringModel(display.getColorByColumn(), coloringLut, null, null );
			}

			display.coloringModel = new SelectionColoringModel( coloringModel, display.selectionModel );
		}
		else
		{
			display.coloringModel = new SelectionColoringModel( display.getLut(), display.selectionModel );
		}
	}

	public void initTableViewer( SegmentationDisplay display  )
	{
		Map< String, String > sourceNameToTableDir = moBIE.getSegmentationTableDirectories( display );
		display.tableViewer = new TableViewer<>( moBIE, display.tableRows, display.selectionModel, display.coloringModel, display.getName(), sourceNameToTableDir, false );
		display.tableViewer.setVisible( display.showTable() );
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	public List< SourceDisplay > getCurrentSourceDisplays()
	{
		return currentSourceDisplays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public ViewSaver getViewsSaver() { return viewSaver; }

	private boolean hasColumnsOutsideProject( AnnotationDisplay annotationDisplay ) {
		if ( annotationDisplay.tableViewer.hasColumnsFromTablesOutsideProject() )
		{
			IJ.log( "Cannot make a view with tables that have columns loaded from the filesystem (not within the project)." );
			return true;
		} else {
			return false;
		}
	}

	private void addManualTransforms( List< SourceTransformer > viewSourceTransforms,
                                      Map<String, SourceAndConverter<?> > sourceNameToSourceAndConverter ) {
        for ( String sourceName: sourceNameToSourceAndConverter.keySet() ) {
            Source<?> source = sourceNameToSourceAndConverter.get( sourceName ).getSpimSource();

            if ( source instanceof LabelSource ) {
                source = ((LabelSource) source).getWrappedSource();
            }
            TransformedSource transformedSource = (TransformedSource) source;

            AffineTransform3D fixedTransform = new AffineTransform3D();
            transformedSource.getFixedTransform( fixedTransform );
            if ( !fixedTransform.isIdentity() ) {
                List<String> sources = new ArrayList<>();
                sources.add( sourceName );
                viewSourceTransforms.add( new AffineSourceTransformer( "manualTransform", fixedTransform.getRowPackedCopy(), sources ) );
            }
        }
    }

	public View getCurrentView( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform ) {

		List< SourceDisplay > viewSourceDisplays = new ArrayList<>();
		List< SourceTransformer > viewSourceTransforms = new ArrayList<>();

		for ( SourceTransformer sourceTransformer : currentSourceTransformers )
			if ( ! viewSourceTransforms.contains( sourceTransformer ) )
				viewSourceTransforms.add( sourceTransformer );

		for ( SourceDisplay sourceDisplay : currentSourceDisplays )
		{
			SourceDisplay currentDisplay = null;

			if ( sourceDisplay instanceof ImageDisplay )
			{
				ImageDisplay imageDisplay = ( ImageDisplay ) sourceDisplay;
				currentDisplay = new ImageDisplay( imageDisplay );
				addManualTransforms( viewSourceTransforms, imageDisplay.sourceNameToSourceAndConverter );
			}
			else if ( sourceDisplay instanceof SegmentationDisplay )
			{
				SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) sourceDisplay;
				if ( hasColumnsOutsideProject( segmentationDisplay ) ) { return null; }
				currentDisplay = new SegmentationDisplay( segmentationDisplay );
				addManualTransforms( viewSourceTransforms, segmentationDisplay.sourceNameToSourceAndConverter );
			}
			else if ( sourceDisplay instanceof RegionDisplay )
			{
				RegionDisplay regionDisplay = ( RegionDisplay ) sourceDisplay;
				if ( hasColumnsOutsideProject( regionDisplay ) ) { return null; }
				currentDisplay = new RegionDisplay( regionDisplay );
			}

			if ( currentDisplay != null )
			{
				viewSourceDisplays.add( currentDisplay );
			}
		}

		if ( includeViewerTransform )
		{
			final BdvHandle bdvHandle = sliceViewer.getBdvHandle();
			AffineTransform3D normalisedViewTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );

			final NormalizedAffineViewerTransform transform = new NormalizedAffineViewerTransform( normalisedViewTransform.getRowPackedCopy(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, transform, isExclusive);
		} else {
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, isExclusive);
		}
	}

	public synchronized void show( String view )
	{
		show( moBIE.getViews().get( view ) );
	}

	public synchronized void show( View view )
	{
		if ( view.isExclusive() )
		{
			removeAllSourceDisplays( true );
		}

		if ( view.getViewerTransform() != null )
		{
			SliceViewLocationChanger.changeLocation( sliceViewer.getBdvHandle(), view.getViewerTransform() );
		}

		openAndTransformSources( view );

		// show the displays
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();
		for ( SourceDisplay sourceDisplay : sourceDisplays )
			showSourceDisplay( sourceDisplay );

		if ( view.getViewerTransform() == null && currentSourceDisplays.size() > 0 && ( view.isExclusive() || currentSourceDisplays.size() == 1 ) )
		{
			final SourceDisplay sourceDisplay = currentSourceDisplays.get( currentSourceDisplays.size() - 1);
			new ViewerTransformAdjuster( sliceViewer.getBdvHandle(), ((AbstractSourceDisplay) sourceDisplay).sourceNameToSourceAndConverter.values().iterator().next() ).run();
		}
	}

	public void openAndTransformSources( View view )
	{
		// fetch the names of all sources that are either shown or to be transformed
		final Set< String > sources = fetchSources( view );
		SourceNameEncoder.addNames( sources );
		final Set< String > rawSources = sources.stream().filter( s -> moBIE.getDataset().sources.containsKey( s ) ).collect( Collectors.toSet() );

		// open all raw sources
		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverters = moBIE.openSourceAndConverters( rawSources );

		// create transformed sources
		final List< SourceTransformer > sourceTransformers = view.getSourceTransforms();
		if ( sourceTransformers != null )
		for ( SourceTransformer sourceTransformer : sourceTransformers )
		{
			currentSourceTransformers.add( sourceTransformer );
			sourceTransformer.transform( sourceNameToSourceAndConverters );
		}

		// Wrap all in a final transformed source.
		// This is so any manual transformations can be
		// retrieved separate from any from sourceTransformers.
		for ( String sourceName : sourceNameToSourceAndConverters.keySet() ) {
			SourceAndConverter<?> sourceAndConverter = new SourceAffineTransformer( sourceNameToSourceAndConverters.get( sourceName ), new AffineTransform3D()).getSourceOut();
			sourceNameToSourceAndConverters.put( sourceName, sourceAndConverter );
		}

		// register all available (transformed) sources in MoBIE
		// this is where the source and segmentation displays will
		// get the sources from
		moBIE.addSourceAndConverters( sourceNameToSourceAndConverters );
	}

	public void adjustViewerTransform( View view )
	{
		final BdvHandle bdvHandle = sliceViewer.getBdvHandle();

		if ( view.getViewerTransform() != null )
		{
			SliceViewLocationChanger.changeLocation( bdvHandle, view.getViewerTransform() );
		}
		else
		{
			if ( view.isExclusive() || currentSourceDisplays.size() == 1 )
			{
				// TODO: rethink what should happen here...
				final SourceDisplay sourceDisplay = currentSourceDisplays.get( currentSourceDisplays.size() > 0 ? currentSourceDisplays.size() - 1 : 0 );
				new ViewerTransformAdjuster( bdvHandle, ((AbstractSourceDisplay) sourceDisplay).sourceNameToSourceAndConverter.values().iterator().next() ).run();
			}
		}
	}

	public Set< String > fetchSources( View view )
	{
		final Set< String > sources = new HashSet<>();
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();

		for ( SourceDisplay sourceDisplay : sourceDisplays )
		{
			sources.addAll( sourceDisplay.getSources() );
		}

		for ( SourceTransformer sourceTransformer : view.getSourceTransforms() )
		{
			sources.addAll( sourceTransformer.getSources() );
		}

		return sources;
	}

	public synchronized void showSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( currentSourceDisplays.contains( sourceDisplay ) ) return;

		if ( sourceDisplay instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationDisplay )
		{
			showSegmentationDisplay( ( SegmentationDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof RegionDisplay )
		{
			showRegionDisplay( ( RegionDisplay ) sourceDisplay );
		}

		userInterface.addSourceDisplay( sourceDisplay );
		currentSourceDisplays.add( sourceDisplay );
	}

	public synchronized void removeAllSourceDisplays( boolean closeImgLoader )
	{
		// create a copy of the currently shown displays...
		final ArrayList< SourceDisplay > currentSourceDisplays = new ArrayList<>( this.currentSourceDisplays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( SourceDisplay sourceDisplay : currentSourceDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			// also close all ImgLoaders to free the cache
			removeSourceDisplay( sourceDisplay, closeImgLoader );
		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay )
	{
		imageDisplay.sliceViewer = sliceViewer;
		imageDisplay.imageSliceView = new ImageSliceView( moBIE, imageDisplay );
		initImageVolumeViewer( imageDisplay );
	}

	// compare with initSegmentationVolumeViewer
	private void initImageVolumeViewer( ImageDisplay imageDisplay )
	{
		imageDisplay.imageVolumeViewer = new ImageVolumeViewer( imageDisplay.sourceNameToSourceAndConverter, universeManager );
		Double[] resolution3dView = imageDisplay.getResolution3dView();
		if ( resolution3dView != null ) {
			imageDisplay.imageVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(imageDisplay.getResolution3dView() ));
		}
		imageDisplay.imageVolumeViewer.showImages( imageDisplay.showImagesIn3d() );

		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceNameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, ImageVolumeViewer.class.getName(), imageDisplay.imageVolumeViewer );
		}
	}

	private void showRegionDisplay( RegionDisplay regionDisplay )
	{
		regionDisplay.sliceViewer = sliceViewer;
		regionDisplay.tableRows = moBIE.createRegionTableRows( regionDisplay );
		regionDisplay.annotatedMaskAdapter = new AnnotatedMaskAdapter( regionDisplay.tableRows );

		regionDisplay.selectionModel = new MoBIESelectionModel<>();
		configureColoringModel( regionDisplay );

		// set selected segments
		if ( regionDisplay.getSelectedRegionIds() != null )
		{
			final List< RegionTableRow > annotatedMasks = regionDisplay.annotatedMaskAdapter.getAnnotatedMasks( regionDisplay.getSelectedRegionIds() );
			regionDisplay.selectionModel.setSelected( annotatedMasks, true );
		}

		showInSliceViewer( regionDisplay );
		initTableViewer( regionDisplay );
		initScatterPlotViewer( regionDisplay );

		SwingUtilities.invokeLater( () ->
		{
			WindowArrangementHelper.bottomAlignWindow( regionDisplay.sliceViewer.getWindow(), regionDisplay.tableViewer.getWindow() );
		} );
	}

	private void initTableViewer( RegionDisplay display )
	{
		Map< String, String > sourceNameToTableDir = moBIE.getRegionTableDirectories( display );
		display.tableViewer = new TableViewer<>( moBIE, display.tableRows, display.selectionModel, display.coloringModel, display.getName(), sourceNameToTableDir, true ).show();
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.sliceViewer = sliceViewer;
		loadTablesAndCreateImageSegments( segmentationDisplay );

		if ( segmentationDisplay.tableRows != null )
		{
			segmentationDisplay.segmentAdapter = new SegmentAdapter( segmentationDisplay.tableRows );
		}
		else
		{
			segmentationDisplay.segmentAdapter = new SegmentAdapter();
		}

		segmentationDisplay.selectionModel = new MoBIESelectionModel<>();
		configureColoringModel( segmentationDisplay );

		// set selected segments
		if ( segmentationDisplay.getSelectedTableRows() != null )
		{
			final List< TableRowImageSegment > segments = segmentationDisplay.segmentAdapter.getSegments( segmentationDisplay.getSelectedTableRows() );
			segmentationDisplay.selectionModel.setSelected( segments, true );
		}

		showInSliceViewer( segmentationDisplay );

		if ( segmentationDisplay.tableRows != null )
		{
			initTableViewer( segmentationDisplay );
			initScatterPlotViewer( segmentationDisplay );

			SwingUtilities.invokeLater( () ->
			{
				WindowArrangementHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
			} );

			initSegmentationVolumeViewer( segmentationDisplay );
		}
	}

	private void loadTablesAndCreateImageSegments( SegmentationDisplay segmentationDisplay )
	{
		final List< String > tables = segmentationDisplay.getTables();

		if ( tables == null ) return;

		// primary table
		moBIE.loadPrimarySegmentsTables( segmentationDisplay );

		// secondary tables
		if ( tables.size() > 1 )
		{
			final List< String > additionalTables = tables.subList( 1, tables.size() );

			moBIE.appendSegmentsTables( segmentationDisplay, additionalTables );
		}

		for ( TableRowImageSegment segment : segmentationDisplay.tableRows )
		{
			if ( segment.labelId() == 0 )
			{
				throw new UnsupportedOperationException( "The table contains rows (image segments) with label index 0, which is not supported and will lead to errors. Please change the table accordingly." );
			}
		}
	}

	private void showInSliceViewer( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.sliceView = new SegmentationSliceView( moBIE, segmentationDisplay );
	}

	private void showInSliceViewer( RegionDisplay regionDisplay )
	{
		regionDisplay.sliceView = new RegionSliceView( moBIE, regionDisplay );
	}

	private void initSegmentationVolumeViewer( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.segmentsVolumeViewer = new SegmentsVolumeViewer<>( segmentationDisplay.selectionModel, segmentationDisplay.coloringModel, segmentationDisplay.sourceNameToSourceAndConverter.values(), universeManager );
		Double[] resolution3dView = segmentationDisplay.getResolution3dView();
		if ( resolution3dView != null ) {
			segmentationDisplay.segmentsVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(segmentationDisplay.getResolution3dView()) );
		}
		segmentationDisplay.segmentsVolumeViewer.showSegments( segmentationDisplay.showSelectedSegmentsIn3d() );
		segmentationDisplay.coloringModel.listeners().add( segmentationDisplay.segmentsVolumeViewer );
		segmentationDisplay.selectionModel.listeners().add( segmentationDisplay.segmentsVolumeViewer );

		for ( SourceAndConverter< ? > sourceAndConverter : segmentationDisplay.sourceNameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, SegmentsVolumeViewer.class.getName(), segmentationDisplay.segmentsVolumeViewer );
		}
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay, boolean closeImgLoader )
	{
		if ( sourceDisplay instanceof AnnotationDisplay )
		{
			final AnnotationDisplay< ? > regionDisplay = ( AnnotationDisplay< ? > ) sourceDisplay;
			regionDisplay.getSliceView().close( closeImgLoader );

			if ( regionDisplay.tableRows != null )
			{
				regionDisplay.tableViewer.close();
				regionDisplay.scatterPlotViewer.close();
				if ( regionDisplay instanceof SegmentationDisplay )
					( ( SegmentationDisplay ) regionDisplay ).segmentsVolumeViewer.close();
			}

		}
		else if ( sourceDisplay instanceof ImageDisplay )
		{
			final ImageDisplay imageDisplay = ( ImageDisplay ) sourceDisplay;
			imageDisplay.imageSliceView.close( false );
		}

		userInterface.removeDisplaySettingsPanel( sourceDisplay );
		currentSourceDisplays.remove( sourceDisplay );

		updateCurrentSourceTransformers();
	}

	private void updateCurrentSourceTransformers()
	{
		// remove any sourceTransformers, where none of its relevant sources are displayed

		// create a copy of the currently shown source transformers, so we don't iterate over a list that we modify
		final ArrayList< SourceTransformer > sourceTransformersCopy = new ArrayList<>( this.currentSourceTransformers ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( SourceDisplay display: currentSourceDisplays )
			currentlyDisplayedSources.addAll( display.getSources() );

		for ( SourceTransformer sourceTransformer: sourceTransformersCopy )
		{
			if ( ! currentlyDisplayedSources.stream().anyMatch( s -> sourceTransformer.getSources().contains( s ) ) )
				currentSourceTransformers.remove( sourceTransformer );
		}
	}

	public Collection< AnnotationDisplay > getAnnotatedRegionDisplays()
	{
		final List< AnnotationDisplay > displays = getCurrentSourceDisplays().stream().filter( s -> s instanceof AnnotationDisplay ).map( s -> ( AnnotationDisplay ) s ).collect( Collectors.toList() );

		return displays;
	}

	public void close()
	{
		removeAllSourceDisplays( true );
		sliceViewer.getBdvHandle().close();
		universeManager.close();
		userInterface.close();
	}
}
