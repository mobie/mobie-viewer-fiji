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
import mobie3.viewer.bdv.view.AnnotatedLabelMaskSliceView;
import mobie3.viewer.bdv.view.SliceViewer;
import mobie3.viewer.color.SelectionColoringModel;
import mobie3.viewer.display.AbstractDisplay;
import mobie3.viewer.display.AnnotationDisplay;
import mobie3.viewer.display.ImageDisplay;
import mobie3.viewer.display.AnnotatedImagesDisplay;
import mobie3.viewer.display.AnnotatedImageSegmentsDisplay;
import mobie3.viewer.display.Display;
import mobie3.viewer.plot.ScatterPlotView;
import mobie3.viewer.segment.TransformedAnnotatedSegment;
import mobie3.viewer.select.MoBIESelectionModel;
import mobie3.viewer.source.AnnotatedImage;
import mobie3.viewer.source.AnnotatedLabelMask;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.Image;
import mobie3.viewer.source.SourceAndConverterAndTables;
import mobie3.viewer.source.TransformedImage;
import mobie3.viewer.table.AnnotatedSegment;
import mobie3.viewer.table.Annotation;
import mobie3.viewer.table.SegmentsAnnData;
import mobie3.viewer.table.TableView;
import mobie3.viewer.transform.AffineTransformation;
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
	private List< Display > currentDisplays;
	private List< Transformation > currentTransformers;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;
	private int numCurrentTables = 0;

	public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentDisplays = new ArrayList<>();
		currentTransformers = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D );
		universeManager = new UniverseManager();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

	private void initScatterPlotView( AnnotationDisplay< ? > display )
	{
		if ( display.tableModel.size() == 0 ) return;

		String[] scatterPlotAxes = display.getScatterPlotAxes();
		display.scatterPlotView = new ScatterPlotView( display.tableModel, display.selectionModel, display.coloringModel, scatterPlotAxes, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotView );
		display.coloringModel.listeners().add( display.scatterPlotView );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotView );

		if ( display.showScatterPlot() )
		{
			display.scatterPlotView.setShowColumnSelectionUI( false );
			display.scatterPlotView.show();
		}
	}

	private static void configureColoringModel( AnnotationDisplay< ? extends TableRow > display )
	{
		if ( display.getColorByColumn() != null )
		{
			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/795
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( display.tableModel.getTableRows() );
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

	public List< Display > getCurrentSourceDisplays()
	{
		return currentDisplays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public ViewSaver getViewsSaver() { return viewSaver; }

	private boolean hasColumnsOutsideProject( AnnotationDisplay annotationDisplay )
	{
		if ( annotationDisplay.tableView.hasColumnsFromTablesOutsideProject() )
		{
			IJ.log( "Cannot make a view with tables that have columns loaded from the filesystem (not within the project)." );
			return true;
		} else {
			return false;
		}
	}

	private void addManualTransforms( List< Transformation > viewSourceTransforms, Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
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
                viewSourceTransforms.add( new AffineTransformation( "manualTransform", fixedTransform.getRowPackedCopy(), sources ) );
            }
        }
    }

	public View createCurrentView( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform )
	{
		List< Display > viewDisplays = new ArrayList<>();
		List< Transformation > viewSourceTransforms = new ArrayList<>();

		for ( Transformation imageTransformation : currentTransformers )
			if ( ! viewSourceTransforms.contains( imageTransformation ) )
				viewSourceTransforms.add( imageTransformation );

		for ( Display display : currentDisplays )
		{
			Display currentDisplay = null;

			if ( display instanceof ImageDisplay )
			{
				ImageDisplay imageDisplay = ( ImageDisplay ) display;
				currentDisplay = new ImageDisplay( imageDisplay );
				addManualTransforms( viewSourceTransforms, imageDisplay.nameToSourceAndConverter );
			}
			else if ( display instanceof AnnotatedImageSegmentsDisplay )
			{
				AnnotatedImageSegmentsDisplay annotatedImageSegmentsDisplay = ( AnnotatedImageSegmentsDisplay ) display;
				currentDisplay = new AnnotatedImageSegmentsDisplay( annotatedImageSegmentsDisplay );
				addManualTransforms( viewSourceTransforms, ( Map ) annotatedImageSegmentsDisplay.nameToSourceAndConverter );
			}
			else if ( display instanceof AnnotatedImagesDisplay )
			{
				currentDisplay = new AnnotatedImagesDisplay( ( AnnotatedImagesDisplay ) display );
			}

			if ( currentDisplay != null )
			{
				viewDisplays.add( currentDisplay );
			}
		}

		if ( includeViewerTransform )
		{
			final BdvHandle bdvHandle = sliceViewer.getBdvHandle();
			AffineTransform3D normalisedViewTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
			final NormalizedAffineViewerTransform transform = new NormalizedAffineViewerTransform( normalisedViewTransform.getRowPackedCopy(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
			return new View(uiSelectionGroup, viewDisplays, viewSourceTransforms, transform, isExclusive);
		}
		else
		{
			return new View(uiSelectionGroup, viewDisplays, viewSourceTransforms, isExclusive);
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

		// init and transform
		initImages( view );

		// display the data
		final List< Display > displays = view.getDisplays();
		for ( Display display : displays )
			show( display );

		// adjust viewer transform
		if ( view.getViewerTransform() == null && currentDisplays.size() > 0 && ( view.isExclusive() || currentDisplays.size() == 1 ) )
		{
			final Display display = currentDisplays.get( currentDisplays.size() - 1);
			new ViewerTransformAdjuster( sliceViewer.getBdvHandle(), (( AbstractDisplay< ? > ) display ).nameToSourceAndConverter.values().iterator().next() ).run();
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

	// initialize and transform
	public void initImages( View view )
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
						final Image image = images.get( name );
						if ( AnnotatedImage.class.isAssignableFrom( image.getClass() ) )
						{
							final AnnotatedImage transformedImage = transform( transformation, ( AnnotatedImage ) image );
							images.put( transformedImage.getName(), transformedImage );
						}
						else
						{
							final TransformedImage transformedImage = new TransformedImage( image, transformation );
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

	private AnnotatedImage< ? extends IntegerType< ? >, ? extends AnnotatedSegment > transform( Transformation transformation, AnnotatedImage image )
	{
		final AnnotatedImage< ? extends IntegerType< ? >, ? extends AnnotatedSegment > annotatedImage = image;
		final TransformedImage< ? extends IntegerType< ? > > transformedLabelMask = new TransformedImage<>( annotatedImage.getLabelMask(), transformation );
		final SegmentsAnnData< TransformedAnnotatedSegment > annData = annotatedImage.getAnnData().transform( transformation );

		final AnnotatedLabelMask< ? extends IntegerType< ? >, ? extends AnnotatedSegment > transformedAnnotatedLabelMask = new AnnotatedLabelMask( transformedLabelMask, annData );
		return transformedAnnotatedLabelMask;
	}

	private SourceAndConverterAndTables createLazySourceAndConverter( String sourceName, SourceAndConverter< ? > parentSource )
	{
		final SourceAndConverterAndTables< ? > sourceAndConverter = new SourceAndConverterAndTables( moBIE, sourceName, parentSource );
		return sourceAndConverter;
	}

	public Set< String > fetchImageNames( View view )
	{
		final Set< String > sources = new HashSet<>();

		for ( Display display : view.getDisplays() )
		{
			for ( String source : display.getSources() )
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

	public synchronized void show( Display display )
	{
		if ( currentDisplays.contains( display ) ) return;

		if ( display instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) display );
		}
		else if ( display instanceof AnnotationDisplay )
		{
			final AnnotationDisplay< ? > annotationDisplay = ( AnnotationDisplay< ? > ) display;

			annotationDisplay.moBIE = moBIE;
			annotationDisplay.sliceViewer = sliceViewer;
			annotationDisplay.selectionModel = new MoBIESelectionModel<>();
			annotationDisplay.initTableModel();

			if ( annotationDisplay instanceof AnnotatedImageSegmentsDisplay )
			{
				showSegmentationDisplay( ( AnnotatedImageSegmentsDisplay ) annotationDisplay );
			}
			else if ( annotationDisplay instanceof AnnotatedImagesDisplay )
			{
				showRegionDisplay( ( AnnotatedImagesDisplay ) annotationDisplay );
			}

			if ( annotationDisplay.tableModel != null )
			{
				initTableView( annotationDisplay );
				initScatterPlotView( annotationDisplay );
				if ( annotationDisplay instanceof AnnotatedImageSegmentsDisplay )
					initSegmentationVolumeViewer( ( AnnotatedImageSegmentsDisplay ) annotationDisplay );
			}
		}

		userInterface.addSourceDisplay( display );
		currentDisplays.add( display );
	}

	public synchronized void removeAllSourceDisplays( boolean closeImgLoader )
	{
		// create a copy of the currently shown displays...
		final ArrayList< Display > currentDisplays = new ArrayList<>( this.currentDisplays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( Display display : currentDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			// also close all ImgLoaders to free the cache
			removeSourceDisplay( display, closeImgLoader );
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

	private void showRegionDisplay( AnnotatedImagesDisplay annotatedImagesDisplay )
	{
		configureColoringModel( annotatedImagesDisplay );

		// set selected segments
		if ( annotatedImagesDisplay.getSelectedRegionIds() != null )
		{
			final List< RegionTableRow > annotatedMasks = annotatedImagesDisplay.tableRowsAdapter.getAnnotatedMasks( annotatedImagesDisplay.getSelectedRegionIds() );
			annotatedImagesDisplay.selectionModel.setSelected( annotatedMasks, true );
		}

		annotatedImagesDisplay.sliceView = new RegionSliceView( moBIE, annotatedImagesDisplay );
		initTableView( annotatedImagesDisplay );
		initScatterPlotView( annotatedImagesDisplay );
		setTablePosition( annotatedImagesDisplay.sliceViewer.getWindow(), annotatedImagesDisplay.tableView.getWindow() );
	}

	private void initTableView( AnnotationDisplay< ? extends Annotation > display )
	{
		display.tableView = new TableView( display );
		display.tableView.show();
		setTablePosition( display.sliceViewer.getWindow(), display.tableView.getWindow() );
		display.selectionModel.listeners().add( display.tableView );
		display.coloringModel.listeners().add( display.tableView );
		numCurrentTables++;
	}

	private void showSegmentationDisplay( AnnotatedImageSegmentsDisplay annotatedImageSegmentsDisplay )
	{
		configureColoringModel( annotatedImageSegmentsDisplay );

		// set selected segments
		if ( annotatedImageSegmentsDisplay.getSelectedSegmentIds() != null )
		{
			final List< TableRowImageSegment > segments = annotatedImageSegmentsDisplay.segmentMapper.getSegments( annotatedImageSegmentsDisplay.getSelectedSegmentIds() );
			annotatedImageSegmentsDisplay.selectionModel.setSelected( segments, true );
		}

		annotatedImageSegmentsDisplay.sliceView = new AnnotatedLabelMaskSliceView( moBIE, annotatedImageSegmentsDisplay );
	}

	private void setTablePosition( Window reference, Window table )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int shift = screenSize.height / 20;
		SwingUtilities.invokeLater( () -> WindowArrangementHelper.bottomAlignWindow( reference, table, ( numCurrentTables - 1 ) * shift ) );
	}

	private void initSegmentationVolumeViewer( AnnotatedImageSegmentsDisplay display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeViewer<>( display.selectionModel, display.coloringModel, display.nameToSourceAndConverter.values(), universeManager );
		Double[] resolution3dView = display.getResolution3dView();
		if ( resolution3dView != null ) {
			display.segmentsVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(display.getResolution3dView()) );
		}
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d(), true );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );

		for ( SourceAndConverter< ? > sourceAndConverter : display.nameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, SegmentsVolumeViewer.class.getName(), display.segmentsVolumeViewer );
		}
	}

	public synchronized void removeSourceDisplay( Display display, boolean closeImgLoader )
	{
		if ( display instanceof AnnotationDisplay )
		{
			final AnnotationDisplay< ? > regionDisplay = ( AnnotationDisplay< ? > ) display;
			regionDisplay.getSliceView().close( closeImgLoader );

			if ( regionDisplay.tableModel != null )
			{
				regionDisplay.tableView.close();
				numCurrentTables--;
				regionDisplay.scatterPlotView.close();
				if ( regionDisplay instanceof AnnotatedImageSegmentsDisplay )
					( ( AnnotatedImageSegmentsDisplay ) regionDisplay ).segmentsVolumeViewer.close();
			}

		}
		else if ( display instanceof ImageDisplay )
		{
			final ImageDisplay imageDisplay = ( ImageDisplay ) display;
			imageDisplay.imageSliceView.close( false );
		}

		userInterface.removeDisplaySettingsPanel( display );
		currentDisplays.remove( display );

		updateCurrentSourceTransformers();
	}

	private void updateCurrentSourceTransformers()
	{
		// remove any sourceTransformers, where none of its relevant sources are displayed

		// create a copy of the currently shown source transformers, so we don't iterate over a list that we modify
		final ArrayList< Transformation > imageTransformersCopy = new ArrayList<>( this.currentTransformers ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( Display display: currentDisplays )
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
