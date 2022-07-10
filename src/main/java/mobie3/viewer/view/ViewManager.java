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
package mobie3.viewer.view;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import mobie3.viewer.MoBIE;
import mobie3.viewer.annotate.RegionTableRow;
import mobie3.viewer.bdv.view.ImageSliceView;
import mobie3.viewer.bdv.view.RegionSliceView;
import mobie3.viewer.bdv.view.SegmentationSliceView;
import mobie3.viewer.bdv.view.SliceViewer;
import mobie3.viewer.color.SelectionColoringModel;
import mobie3.viewer.display.AbstractSourceDisplay;
import mobie3.viewer.display.AnnotationDisplay;
import mobie3.viewer.display.ImageDisplay;
import mobie3.viewer.display.RegionDisplay;
import mobie3.viewer.display.SegmentationDisplay;
import mobie3.viewer.display.SourceDisplay;
import mobie3.viewer.plot.ScatterPlotViewer;
import mobie3.viewer.segment.TransformedSegmentRow;
import mobie3.viewer.select.MoBIESelectionModel;
import mobie3.viewer.source.AnnotatedImage;
import mobie3.viewer.source.AnnotatedLabelMask;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.Image;
import mobie3.viewer.source.SourceAndConverterAndTables;
import mobie3.viewer.source.TransformedImage;
import mobie3.viewer.table.SegmentRow;
import mobie3.viewer.table.SegmentsAnnData;
import mobie3.viewer.table.TableViewer;
import mobie3.viewer.transform.AffineImageTransformation;
import mobie3.viewer.transform.NormalizedAffineViewerTransform;
import mobie3.viewer.transform.SliceViewLocationChanger;
import mobie3.viewer.transform.Transformation;
import mobie3.viewer.transform.TransformHelper;
import mobie3.viewer.ui.UserInterface;
import mobie3.viewer.ui.WindowArrangementHelper;
import mobie3.viewer.view.save.ViewSaver;
import mobie3.viewer.volume.ImageVolumeViewer;
import mobie3.viewer.volume.SegmentsVolumeViewer;
import mobie3.viewer.volume.UniverseManager;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.apache.commons.lang.ArrayUtils;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewManager
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private List< SourceDisplay > currentSourceDisplays;
	private List< Transformation > currentTransformers;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;
	private int numCurrentTables = 0;

	public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentSourceDisplays = new ArrayList<>();
		currentTransformers = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D );
		universeManager = new UniverseManager();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

	private void initScatterPlotViewer( AnnotationDisplay< ? > display )
	{
		if ( display.tableRows.size() == 0 ) return;

		String[] scatterPlotAxes = display.getScatterPlotAxes();
		display.scatterPlotViewer = new ScatterPlotViewer( display.tableRows, display.selectionModel, display.selectionColoringModel, scatterPlotAxes, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.selectionColoringModel.listeners().add( display.scatterPlotViewer );
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
			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/795
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( display.tableRows.getTableRows() );
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

			display.selectionColoringModel = new SelectionColoringModel( coloringModel, display.selectionModel );
		}
		else
		{
			display.selectionColoringModel = new SelectionColoringModel( display.getLut(), display.selectionModel );
		}
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

	private boolean hasColumnsOutsideProject( AnnotationDisplay annotationDisplay )
	{
		if ( annotationDisplay.tableViewer.hasColumnsFromTablesOutsideProject() )
		{
			IJ.log( "Cannot make a view with tables that have columns loaded from the filesystem (not within the project)." );
			return true;
		} else {
			return false;
		}
	}

	private void addManualTransforms( List< Transformation > viewSourceTransforms,
                                      Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
        for ( String sourceName: sourceNameToSourceAndConverter.keySet() ) {
            Source< ? > source = sourceNameToSourceAndConverter.get( sourceName ).getSpimSource();

            if ( source instanceof BoundarySource ) {
                source = (( BoundarySource ) source).getWrappedSource();
            }
            TransformedSource transformedSource = (TransformedSource) source;

            AffineTransform3D fixedTransform = new AffineTransform3D();
            transformedSource.getFixedTransform( fixedTransform );
            if ( !fixedTransform.isIdentity() ) {
                List<String> sources = new ArrayList<>();
                sources.add( sourceName );
                viewSourceTransforms.add( new AffineImageTransformation( "manualTransform", fixedTransform.getRowPackedCopy(), sources ) );
            }
        }
    }

	public View createCurrentView( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform )
	{
		List< SourceDisplay > viewSourceDisplays = new ArrayList<>();
		List< Transformation > viewSourceTransforms = new ArrayList<>();

		for ( Transformation imageTransformation : currentTransformers )
			if ( ! viewSourceTransforms.contains( imageTransformation ) )
				viewSourceTransforms.add( imageTransformation );

		for ( SourceDisplay sourceDisplay : currentSourceDisplays )
		{
			SourceDisplay currentDisplay = null;

			if ( sourceDisplay instanceof ImageDisplay )
			{
				ImageDisplay imageDisplay = ( ImageDisplay ) sourceDisplay;
				currentDisplay = new ImageDisplay( imageDisplay );
				addManualTransforms( viewSourceTransforms, imageDisplay.nameToSourceAndConverter );
			}
			else if ( sourceDisplay instanceof SegmentationDisplay )
			{
				SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) sourceDisplay;
				currentDisplay = new SegmentationDisplay( segmentationDisplay );
				addManualTransforms( viewSourceTransforms, ( Map ) segmentationDisplay.nameToSourceAndConverter );
			}
			else if ( sourceDisplay instanceof RegionDisplay )
			{
				currentDisplay = new RegionDisplay( ( RegionDisplay ) sourceDisplay );
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
		}
		else
		{
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

		openAndTransformImages( view );

		// show the displays
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();
		for ( SourceDisplay sourceDisplay : sourceDisplays )
			showSourceDisplay( sourceDisplay );

		if ( view.getViewerTransform() == null && currentSourceDisplays.size() > 0 && ( view.isExclusive() || currentSourceDisplays.size() == 1 ) )
		{
			final SourceDisplay sourceDisplay = currentSourceDisplays.get( currentSourceDisplays.size() - 1);
			new ViewerTransformAdjuster( sliceViewer.getBdvHandle(), (( AbstractSourceDisplay< ? > ) sourceDisplay).nameToSourceAndConverter.values().iterator().next() ).run();
		}

		// trigger rendering of source name overlay
		getSliceViewer().getSourceNameRenderer().transformChanged( sliceViewer.getBdvHandle().getViewerPanel().state().getViewerTransform() );

		// adapt time point
		if ( view.getViewerTransform() != null )
		{
			// This needs to be done after adding all the sources,
			// because otherwise the requested timepoint may not yet
			// exist in BDV
			SliceViewLocationChanger.adaptTimepoint( sliceViewer.getBdvHandle(), view.getViewerTransform() );
		}
	}

	public void openAndTransformImages( View view )
	{
		// fetch names of all sources that are
		// either to be shown or to be transformed
		final Set< String > imageNames = fetchImageNames( view );
		if ( imageNames.size() == 0 ) return;

		// determine "raw" images that can be directly opened
		// (some others may only be available via a transformation)
		final List< String > rawImageNames = imageNames.stream().filter( s -> ( moBIE.getDataset().sources.containsKey( s ) ) ).collect( Collectors.toList() );

		// init images
		final HashMap< String, Image< ? > > images = moBIE.initImages( rawImageNames );

		// transform images
		// this may create new images with new names
		final List< Transformation > transformers = view.getImageTransformers();
		if ( transformers != null )
		{
			for ( Transformation transformation : transformers )
			{
				currentTransformers.add( transformation );

				for ( String name : images.keySet() )
				{
					if ( transformation.getTargetImages().contains( name ) )
					{
						final Image< ? > image = images.get( name );
						if ( AnnotatedImage.class.isAssignableFrom( image.getClass() ) )
						{
							final AnnotatedImage< ? extends IntegerType< ? >, ? extends SegmentRow > annotatedImage = ( AnnotatedImage) image;
							final TransformedImage< ? extends IntegerType< ? > > transformedLabelMask = new TransformedImage<>( annotatedImage.getLabelMask(), transformation );
							final SegmentsAnnData< TransformedSegmentRow > annData = annotatedImage.getAnnData().transform( transformation );

							final AnnotatedLabelMask< ? extends IntegerType< ? >, ? extends SegmentRow > transformedAnnotatedLabelMask = new AnnotatedLabelMask( transformedLabelMask, annData );
							images.put( transformedLabelMask.getName(), transformedAnnotatedLabelMask );
						}
						else
						{
							final TransformedImage< ? > transformedImage = new TransformedImage<>( image, transformation );
							images.put( transformedImage.getName(), transformedImage );
						}
					}
				}

			}
		}


		// register all available (transformed) sources in MoBIE
		// this is where the source and segmentation displays will
		// get the images from
		moBIE.addImages( images );
	}

	private SourceAndConverterAndTables createLazySourceAndConverter( String sourceName, SourceAndConverter< ? > parentSource )
	{
		final SourceAndConverterAndTables< ? > sourceAndConverter = new SourceAndConverterAndTables( moBIE, sourceName, parentSource );
		return sourceAndConverter;
	}

	public Set< String > fetchImageNames( View view )
	{
		final Set< String > sources = new HashSet<>();

		for ( SourceDisplay sourceDisplay : view.getSourceDisplays() )
		{
			for ( String source : sourceDisplay.getSources() )
			{
				sources.add( source );
			}
		}

		for ( Transformation imageTransformation : view.getImageTransformers() )
		{
			final List< String > sourceTransformerSources = imageTransformation.getTargetImages();
			for ( String source : sourceTransformerSources )
			{
				sources.add( source );
			}
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
		else if ( sourceDisplay instanceof AnnotationDisplay )
		{
			final AnnotationDisplay< ? > annotationDisplay = ( AnnotationDisplay< ? > ) sourceDisplay;

			annotationDisplay.moBIE = moBIE;
			annotationDisplay.sliceViewer = sliceViewer;
			annotationDisplay.selectionModel = new MoBIESelectionModel<>();
			annotationDisplay.initTableRows();

			if ( annotationDisplay instanceof SegmentationDisplay )
			{
				showSegmentationDisplay( ( SegmentationDisplay ) annotationDisplay );
			}
			else if ( annotationDisplay instanceof RegionDisplay )
			{
				showRegionDisplay( ( RegionDisplay ) annotationDisplay );
			}

			if ( annotationDisplay.tableRows != null )
			{
				initTableViewer( annotationDisplay );
				initScatterPlotViewer( annotationDisplay );
				if ( annotationDisplay instanceof SegmentationDisplay )
					initSegmentationVolumeViewer( ( SegmentationDisplay ) annotationDisplay );
			}
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
	private void initImageVolumeViewer( ImageDisplay< ? > imageDisplay )
	{
		imageDisplay.imageVolumeViewer = new ImageVolumeViewer( imageDisplay.nameToSourceAndConverter, universeManager );
		Double[] resolution3dView = imageDisplay.getResolution3dView();
		if ( resolution3dView != null ) {
			imageDisplay.imageVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(imageDisplay.getResolution3dView() ));
		}
		imageDisplay.imageVolumeViewer.showImages( imageDisplay.showImagesIn3d() );

		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.nameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, ImageVolumeViewer.class.getName(), imageDisplay.imageVolumeViewer );
		}
	}

	private void showRegionDisplay( RegionDisplay regionDisplay )
	{
		configureColoringModel( regionDisplay );

		// set selected segments
		if ( regionDisplay.getSelectedRegionIds() != null )
		{
			final List< RegionTableRow > annotatedMasks = regionDisplay.tableRowsAdapter.getAnnotatedMasks( regionDisplay.getSelectedRegionIds() );
			regionDisplay.selectionModel.setSelected( annotatedMasks, true );
		}

		regionDisplay.sliceView = new RegionSliceView( moBIE, regionDisplay );
		initTableViewer( regionDisplay );
		initScatterPlotViewer( regionDisplay );
		setTablePosition( regionDisplay.sliceViewer.getWindow(), regionDisplay.tableViewer.getWindow() );
	}

	private void initTableViewer( AnnotationDisplay< ? extends TableRow > display )
	{
		display.tableViewer = new TableViewer( moBIE, display );
		display.tableViewer.show();
		setTablePosition( display.sliceViewer.getWindow(), display.tableViewer.getWindow() );
		display.selectionModel.listeners().add( display.tableViewer );
		display.selectionColoringModel.listeners().add( display.tableViewer );
		numCurrentTables++;
	}

	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		configureColoringModel( segmentationDisplay );

		// set selected segments
		if ( segmentationDisplay.getSelectedSegmentIds() != null )
		{
			final List< TableRowImageSegment > segments = segmentationDisplay.tableRowsAdapter.getSegments( segmentationDisplay.getSelectedSegmentIds() );
			segmentationDisplay.selectionModel.setSelected( segments, true );
		}

		segmentationDisplay.sliceView = new SegmentationSliceView( moBIE, segmentationDisplay );
	}

	private void setTablePosition( Window reference, Window table )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int shift = screenSize.height / 20;
		SwingUtilities.invokeLater( () -> WindowArrangementHelper.bottomAlignWindow( reference, table, ( numCurrentTables - 1 ) * shift ) );
	}

	private void initSegmentationVolumeViewer( SegmentationDisplay display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeViewer<>( display.selectionModel, display.selectionColoringModel, display.nameToSourceAndConverter.values(), universeManager );
		Double[] resolution3dView = display.getResolution3dView();
		if ( resolution3dView != null ) {
			display.segmentsVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(display.getResolution3dView()) );
		}
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d(), true );
		display.selectionColoringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );

		for ( SourceAndConverter< ? > sourceAndConverter : display.nameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, SegmentsVolumeViewer.class.getName(), display.segmentsVolumeViewer );
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
				numCurrentTables--;
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
		final ArrayList< Transformation > imageTransformersCopy = new ArrayList<>( this.currentTransformers ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( SourceDisplay display: currentSourceDisplays )
			currentlyDisplayedSources.addAll( display.getSources() );

		for ( Transformation imageTransformation : imageTransformersCopy )
		{
			if ( ! currentlyDisplayedSources.stream().anyMatch( s -> imageTransformation.getTargetImages().contains( s ) ) )
				currentTransformers.remove( imageTransformation );
		}
	}

	// TODO: typing ( or remove )
	public Collection< AnnotationDisplay< ? > > getAnnotationDisplays()
	{
		final List< AnnotationDisplay< ? > > collect = getCurrentSourceDisplays().stream().filter( s -> s instanceof AnnotationDisplay ).map( s -> ( AnnotationDisplay< ? > ) s ).collect( Collectors.toList() );

		return collect;
	}

	public void close()
	{
		IJ.log( "Closing BDV..." );
		removeAllSourceDisplays( true );
		sliceViewer.getBdvHandle().close();
		IJ.log( "Closing 3D Viewer..." );
		universeManager.close();
		IJ.log( "Closing UI..." );
		userInterface.close();
	}
}
