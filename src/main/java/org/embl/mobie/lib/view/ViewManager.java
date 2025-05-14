/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.overlay.ImageNameOverlay;
import org.embl.mobie.lib.bdv.view.AnnotationSliceView;
import org.embl.mobie.lib.bdv.view.ImageSliceView;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.bvv.BigVolumeViewerMoBIE;
import org.embl.mobie.lib.color.*;
import org.embl.mobie.lib.color.lut.ColumnARGBLut;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.plot.ScatterPlotSettings;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.*;
import org.embl.mobie.lib.serialize.transformation.*;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.table.*;
import org.embl.mobie.lib.transform.ImageTransformer;
import org.embl.mobie.lib.transform.viewer.*;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.view.delete.ViewDeleter;
import org.embl.mobie.ui.UserInterface;
import org.embl.mobie.ui.WindowArrangementHelper;
import org.embl.mobie.lib.view.save.ViewSaver;
import org.embl.mobie.lib.volume.ImageVolumeViewer;
import org.embl.mobie.lib.volume.SegmentVolumeViewer;
import org.embl.mobie.lib.volume.UniverseManager;
import org.embl.mobie.ui.MoBIEWindowManager;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ViewManager
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private List< Display > currentDisplays;
	private final UniverseManager universeManager;
	private final BigVolumeViewerMoBIE bigVolumeViewer;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;
	private final ViewDeleter viewDeleter;

	public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentDisplays = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D );
		universeManager = new UniverseManager();
		bigVolumeViewer = new BigVolumeViewerMoBIE();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		viewDeleter = new ViewDeleter( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}


	public static View createImageView(
			Image< ? > image,
			String imageName,
			@Nullable Transformation transformation,
			String viewDescription )
	{
		ArrayList< Transformation > transformations = MoBIEHelper.fetchAddedImageTransformations( image );
		if ( transformation != null )
			transformations.add( transformation );

		Display< ? > display;
		if ( image instanceof AnnotationImage )
		{
            display = new SegmentationDisplay<>( imageName, imageName );
		}
		else
		{
			ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, imageName );
			SourceAndConverter< ? > sourceAndConverter = DataStore.sourceToImage().inverse().get( image );
			if ( sourceAndConverter != null )
			{
				// this image has been previously displayed
				// and we fetch the display settings
				imageDisplay.setDisplaySettings( sourceAndConverter );
			}
			display = imageDisplay;
		}

		View view = new View(
				imageName,
                ( String ) null, // to be determined by the user in below dialog
				Collections.singletonList( display ),
				transformations,
				null,
				false,
				viewDescription );

		view.setExclusive( false );

		return view;
	}

//	@Deprecated // use createTransformedImageView instead
//	public static void createTransformedSourceView(
//			SourceAndConverter< ? > sac,
//			String imageName,
//			Transformation transformation,
//			String viewDescription )
//	{
//		// TODO: Use TransformHelper.fetchAddedTransformations( Image<?> image ) instead
//		// TODO: Make this work for label images https://github.com/mobie/mobie-viewer-fiji/issues/1126
//		ArrayList< Transformation > transformations = TransformHelper.fetchAddedSourceTransformations( sac.getSpimSource() );
//		transformations.add( transformation );
//
//		ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, imageName );
//		imageDisplay.setDisplaySettings( sac );
//
//		View view = new View(
//				imageName,
//				null, // to be determined by the user in below dialog
//				Collections.singletonList( imageDisplay ),
//				transformations,
//				null,
//				false,
//				viewDescription );
//
//		MoBIE.getInstance().getViewManager().getViewsSaver().saveViewDialog( view );
//
//		MoBIE.getInstance().getViewManager().show( view );
//	}

	private void initScatterPlotView( AbstractAnnotationDisplay< ? > display )
	{
		final ScatterPlotSettings settings = new ScatterPlotSettings( display.getScatterPlotAxes() );
		display.scatterPlotView = new ScatterPlotView( display.getAnnData().getTable(), display.selectionModel, display.coloringModel, settings );
		display.selectionModel.listeners().add( display.scatterPlotView );
		display.coloringModel.listeners().add( display.scatterPlotView );
		display.sliceViewer.getBdvHandle().getViewerPanel().timePointListeners().add( display.scatterPlotView );

		if ( display.showScatterPlot() )
			display.scatterPlotView.show( false );
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

	public ViewDeleter getViewsDeleter() { return viewDeleter; }

	private void addImageTransforms( List< Transformation > transformations,
									 List< ? extends Image< ? > > images )
	{
		images.forEach( image ->
		{
			ArrayList< Transformation > imageTransformations = MoBIEHelper.fetchAddedImageTransformations( image );
			transformations.addAll( imageTransformations );
		});
    }

	public View createViewFromCurrentState()
	{
		// Create serializable copies of the current displays
		List< Display< ? > > displays = new ArrayList<>();
		List< Transformation > transformations = new ArrayList<>();
		for ( Display< ? > display : currentDisplays )
		{
			if ( display instanceof ImageDisplay )
				displays.add( new ImageDisplay( ( ImageDisplay ) display ) );
			else if ( display instanceof SegmentationDisplay )
				displays.add( new SegmentationDisplay( ( SegmentationDisplay ) display ) );
			else if ( display instanceof RegionDisplay )
				displays.add( new RegionDisplay( ( RegionDisplay ) display ) );
			else if ( display instanceof SpotDisplay )
				displays.add( new SpotDisplay( ( SpotDisplay) display ) );
			else
				throw new UnsupportedOperationException( "Serialisation of a " + display.getClass().getName() + " is not yet supported." );

			addImageTransforms( transformations, display.images() );
		}

		// the parameters that are null must be later set via the view's setter methods
		return new View( null, ( String[] ) null, displays, transformations, null, true, "" );
	}

	public synchronized void show( String viewName )
	{
		final View view = moBIE.getViews().get( viewName );
		view.setName( viewName );
		show( view );
	}

	public synchronized void show( View view )
	{
		final long startTime = System.currentTimeMillis();

		IJ.log("Opening view \"" + view.getName() + "\"..." );

		if ( view.isExclusive() )
		{
			removeAllSourceDisplays( true );
		 	DataStore.clearImages();
			MoBIEWindowManager.closeAllWindows();
		}

		final boolean viewerWasEmpty = currentDisplays.isEmpty();

		// Init and transform the data of this view.
		// Currently, data is reloaded every time a view is shown.
		// This is a bit expensive, but simpler and has the
		// advantage that one could change the data on disk
		// and use MoBIE to interactively view changes
		initData( view );

		// set the viewer transform *after* initialising the data
		// as the data may be needed to know where to
		// focus to; but do this *before* adding the data
		// to BDV to avoid premature loading of too much data
		//
		if ( view.getViewerTransform() != null )
		{
			final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
			final ViewerTransform viewerTransform = view.getViewerTransform();
			if ( viewerTransform instanceof ImageZoomViewerTransform )
			{
				final String imageName = ( ( ImageZoomViewerTransform ) viewerTransform ).getImageName();
				final RealMaskRealInterval mask = DataStore.getImage( imageName ).getMask();
				final AffineTransform3D transform = MoBIEHelper.getIntervalViewerTransform( bdvHandle, mask );
				ViewerTransformChanger.apply( bdvHandle, transform, 0 );
			}
			else
			{
				ViewerTransformChanger.apply( bdvHandle, viewerTransform );
			}
		}

		// display the data
		final List< Display< ? > > displays = view.displays();
		for ( Display< ? > display : displays )
			show( display );

		// adjust viewer transform to accommodate the displayed sources.
		// note that if {@code view.getViewerTransform() != null}
		// the viewer transform has already been adjusted above.
		if ( view.getViewerTransform() == null && !currentDisplays.isEmpty() && viewerWasEmpty )
		{
			AffineTransform3D viewerTransform = MoBIEViewerTransformAdjuster.getViewerTransform(
					sliceViewer.getBdvHandle(),
					currentDisplays.get( 0 ) );
			sliceViewer.getBdvHandle().getViewerPanel().state().setViewerTransform( viewerTransform );
		}

		// adapt time point
		if ( view.getViewerTransform() != null )
		{
			// this needs to be done after adding all the sources,
			// because otherwise the requested timepoint may not yet
			// exist in BDV
			ViewerTransformChanger.adaptTimepoint( sliceViewer.getBdvHandle(), view.getViewerTransform() );
		}

		// overlay names
		final ImageNameOverlay imageNameOverlay = getSliceViewer().getImageNameOverlay();
		userInterface.setImageNameOverlay( imageNameOverlay );
		imageNameOverlay.setActive( view.overlayNames() );

		IJ.log("...done in " + (System.currentTimeMillis() - startTime) + " ms." );
		if ( view.getDescription() != null )
			IJ.log( "View description: \"" + view.getDescription() + "\"" );

	}

	// initialize and transform
	public void initData( View view )
	{
		// fetch names of all data sources that are
		// either to be shown directly or to be transformed
		final Map< String, Object > sourceToTransformOrDisplay = view.getSources();
		if ( sourceToTransformOrDisplay.size() == 0 ) return;

		// instantiate source that can be directly opened
		// (other sources may be created later,
		// by a display or transformation)
		List< DataSource > dataSources = moBIE.getDataSources( sourceToTransformOrDisplay.keySet() );

		// if a view is created on the fly in a running project, e.g. due to an image registration
		// the data sources may already be present and thus do not need to be instantiated
		// HOWEVER: the issue here is that then an image may exist already and a transformation is applied twice (see below)
		//    example: public class OpenPaoloFirstTable  => view the first image twice
		// Thus, currently we reload the data
		// dataSources = dataSources.stream()
		//		.filter( ds -> ! DataStore.containsImage( ds.getName() ) )
		//		.collect( Collectors.toList() );

		for ( DataSource dataSource : dataSources )
		{
			String dataSourceName = dataSource.getName();

			final Object transformOrDisplay = sourceToTransformOrDisplay.get( dataSourceName );
			if ( transformOrDisplay instanceof MergedGridTransformation )
			{
				final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformOrDisplay;
				if ( mergedGridTransformation.metadataSource != null )
				{
                    dataSource.preInit( dataSourceName.equals( mergedGridTransformation.metadataSource ) );
				}
				else // no metadata source specified, use the first in the grid as metadata source
				{
					final String firstImageInGrid = mergedGridTransformation.getSources().get( 0 );
                    dataSource.preInit( dataSourceName.equals( firstImageInGrid ) );
				}
			}
			else
			{
				dataSource.preInit( true );
			}
		}

		if ( ! dataSources.isEmpty() )
			moBIE.initDataSources( dataSources );

		// transform images
		// this may create new images with new names
		final List< Transformation > transformations = view.transformations();
		if ( transformations != null )
		{
			// FIXME: the issue here is that then an image may exist already and a transformation is applied twice (see above)
			for ( Transformation transformation : transformations )
			{
				if ( transformation instanceof ImageTransformation )
				{
					final List< ? extends Image< ? > > images = DataStore.getImageList( transformation.getSources() );

					if ( transformation instanceof AffineTransformation )
					{
						final AffineTransformation affineTransformation = ( AffineTransformation ) transformation;

						for ( Image< ? > image : images )
						{
							Image< ? > transformedImage = ImageTransformer.affineTransform( image, affineTransformation );
							DataStore.addImage( transformedImage );
						}
					}
					else if ( transformation instanceof CropTransformation )
					{
						final CropTransformation cropTransformation = ( CropTransformation ) transformation;

						for ( Image< ? > image : images )
						{
							// TODO: move this into the ImageTransformer class
							final CroppedImage< ? > croppedImage = new CroppedImage<>(
									image,
									cropTransformation.getTransformedImageName( image.getName() ),
									cropTransformation.min,
									cropTransformation.max,
									cropTransformation.centerAtOrigin );
							DataStore.addImage( croppedImage );
						}
					}
					else if ( transformation instanceof TimepointsTransformation )
					{
						final TimepointsTransformation timepointsTransformation = ( TimepointsTransformation ) transformation;

						for ( Image< ? > image : images )
						{
							DataStore.addImage( ImageTransformer.timeTransform( image, timepointsTransformation ) );
						}
					}
					else if ( transformation instanceof InterpolatedAffineTransformation )
					{
						InterpolatedAffineTransformation interpolatedAffineTransformation = ( InterpolatedAffineTransformation ) transformation;

						for ( Image< ? > image : images )
						{
							DataStore.addImage( ImageTransformer.interpolatedAffineTransform( image, interpolatedAffineTransformation ) );
						}
					}
					else
					{
						throw new UnsupportedOperationException( "Transformations of type " + transformation.getClass().getName() + " are not yet implemented.");
					}
				}
				else // not an ImageTransformation
				{
					if ( transformation instanceof MergedGridTransformation )
					{
						// TODO: move this into the ImageTransformer class
						final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformation;

						List< ? extends Image< ? > > images = DataStore.getImageList( mergedGridTransformation.getSources() );

//						if ( images.size() == 1 )
//						{
//							DataStore.addImage( images.get( 0 ) );
//							continue;
//						}

						// Fetch grid metadata image
						Image< ? > metadataImage = ( mergedGridTransformation.metadataSource == null ) ? images.get( 0 ) : DataStore.getImage( mergedGridTransformation.metadataSource );

						// Create the stitched grid image
						//
						if ( images.get( 0 ) instanceof AnnotationImage )
						{
							final StitchedAnnotationImage< ? extends Annotation > annotatedStitchedImage
									= new StitchedAnnotationImage<>(
											( List ) images,
											( Image ) metadataImage,
											mergedGridTransformation.positions,
											mergedGridTransformation.getName(),
											mergedGridTransformation.margin );

							if ( ! mergedGridTransformation.lazyLoadTables
									&& annotatedStitchedImage.getAnnData().getTable() instanceof ConcatenatedAnnotationTableModel )
							{
								// force loading of all tables to enable meaningful
								// row sorting and creating a meaningful scatterplot
								final ConcatenatedAnnotationTableModel< ? extends Annotation > concatenatedTableModel = ( ConcatenatedAnnotationTableModel ) annotatedStitchedImage.getAnnData().getTable();
								concatenatedTableModel.loadAllTables();
							}

							DataStore.addImage( annotatedStitchedImage );
						}
						else
						{
							DataStore.addImage( new StitchedImage<>(
									( List ) images,
									( Image ) metadataImage,
									mergedGridTransformation.positions,
									mergedGridTransformation.getName(),
									mergedGridTransformation.margin )
							 );
						}
					}
					else if ( transformation instanceof GridTransformation )
					{
						final List< ? extends Image< ? > > translatedImages =
								GridTransformation.translateImages( ( GridTransformation ) transformation );

						DataStore.putImages( translatedImages );
					}
					else
					{
						throw new UnsupportedOperationException( "Transformations of type " + transformation.getClass().getName() + " are not yet implemented.");
					}
				}
			}
		}

		// Instantiate {@code RegionDisplay}s
		// This cannot be done already in MoBIE.initData()
		// because we need to wait until all images are initialised
		for ( Display< ? > display : view.displays() )
		{
			if ( display instanceof RegionDisplay )
			{
				// Build the image that visualises this RegionDisplay
				// The logic here is not ideal:
				// https://github.com/mobie/mobie-viewer-fiji/issues/818
				// ...as normally a display should visualise an existing image
				// here however the display creates the image
				// The imageNames that are referred to here must exist in this view.
				// Thus the {@code RegionAnnotationImage} must be build
				// *after* the above transformations,
				// which may create new images
				// that could be referred to here.
				AnnData< AnnotatedRegion > annData = new RegionDisplayAnnDataCreator(
						moBIE, ( RegionDisplay< ? > ) display ).createAnnData();
				final RegionAnnotationImage< AnnotatedRegion > regionAnnotationImage =
						new RegionAnnotationImage( ( RegionDisplay< ? > ) display, annData );
				// The region image has the same name as the display,
				// thus it can be identified later to be the image that
				// will be shown by this display (via {@code regionDisplay.getSources()})
				DataStore.addImage( regionAnnotationImage );
			}
		}
	}

	public synchronized < A extends Annotation > void show( Display< ? > display )
	{
		if ( currentDisplays.contains( display ) ) return;

		// remove previous images
		// this is necessary because the object identity of the images
		// changes when they are reloaded, which
		// currently is the case when (repeatedly) selecting a view
		display.images().clear();

		if ( display instanceof AbstractDisplay )
		{
			(( AbstractDisplay< ? > ) display).sliceViewer = sliceViewer;
			(( AbstractDisplay< ? > ) display).bigVolumeViewer = bigVolumeViewer;
		}

		if ( display instanceof ImageDisplay )
		{
			for ( String name : display.getSources() )
				display.images().add( ( Image ) DataStore.getImage( name ) );
			showImageDisplay( ( ImageDisplay ) display );
		}
		else if ( display instanceof AbstractAnnotationDisplay )
		{
			final AbstractAnnotationDisplay< A > annotationDisplay = ( AbstractAnnotationDisplay ) display;

			for ( String name : display.getSources() )
			{
				// all sources are modelled as images
				final Image< ? > image = DataStore.getImage( name );
				annotationDisplay.images().add( ( Image< AnnotationType< A > > ) image );
			}
			annotationDisplay.combineAnnData();

			// load additional tables (to be merged)
			final List< String > requestedTableChunks = annotationDisplay.getRequestedTableChunks();
			if ( requestedTableChunks != null )
				for ( String tableChunk : requestedTableChunks )
				{
					final AnnotationTableModel< A > tableModel = annotationDisplay.getAnnData().getTable();
					tableModel.loadTableChunk( tableChunk );
				}

			// set selected segments
			//
			final Set< String > selectedAnnotationIds = annotationDisplay.selectedAnnotationIds();
			if ( selectedAnnotationIds != null )
			{
				final List< A > selectedAnnotations = annotationDisplay
						.getAnnData().getTable().annotations().stream()
						.filter( a -> selectedAnnotationIds.contains( a.uuid() ) )
						.collect( Collectors.toList() );
				annotationDisplay.selectionModel.setSelected( selectedAnnotations, true );
			}

			// configure coloring model
			//
			String lut = annotationDisplay.getLut();

			if ( annotationDisplay.getValueLimits() == null )
			{
				CategoricalAnnotationColoringModel< Annotation > coloringModel = ColoringModels.createCategoricalModel( annotationDisplay.getColoringColumnName(), lut, LUTs.TRANSPARENT );

				if ( LUTs.getLut( lut ) instanceof ColumnARGBLut )
				{
					// note that this currently forces loading of the table(s)
					// for big data this may need some improvement
					final AnnotationTableModel< A > table = annotationDisplay.getAnnData().getTable();
					for ( A annotation : table.annotations() )
					{
						String argbString = annotation.getValue( annotationDisplay.getColoringColumnName() ).toString();

						if ( argbString.equals("") )
							continue;

						final ARGBType argbType = ColorHelper.getARGBType( argbString );

						coloringModel.assignColor( argbString, argbType.get() );
					}
				}

				coloringModel.setRandomSeed( annotationDisplay.getRandomColorSeed() );

				annotationDisplay.coloringModel = new MobieColoringModel(
						coloringModel,
						annotationDisplay.selectionModel,
						annotationDisplay.getSelectionColor(),
						annotationDisplay.getOpacityNotSelected() );
			}
			else
			{
				NumericAnnotationColoringModel< Annotation > coloringModel
						= ColoringModels.createNumericModel(
								annotationDisplay.getColoringColumnName(),
								lut,
								annotationDisplay.getValueLimits(),
						 true
							);

				annotationDisplay.coloringModel = new MobieColoringModel( coloringModel, annotationDisplay.selectionModel, annotationDisplay.getSelectionColor(), annotationDisplay.getOpacityNotSelected() );
			}

			// show the data
			//
			annotationDisplay.sliceView = new AnnotationSliceView<>( moBIE, annotationDisplay );

			if( annotationDisplay.getAnnData().getTable().annotations().size() > 0 )
			{
				// if there are no annotations to start with there is no table
				// and the table is built on the fly.
				// in this case we do not want to show the table
				// https://github.com/mobie/mobie-viewer-fiji/issues/1184
				initTableView( annotationDisplay );
				initScatterPlotView( annotationDisplay );
			}

			if ( annotationDisplay instanceof SegmentationDisplay )
				initSegmentVolumeViewer( ( SegmentationDisplay ) annotationDisplay );
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
			removeDisplay( display, closeImgLoader );
		}
	}

	public void showImageDisplay( ImageDisplay display )
	{
		display.imageSliceView = new ImageSliceView( moBIE, display );
		initImageVolumeViewer( display );
	}

	// compare with initSegmentationVolumeViewer
	private void initImageVolumeViewer( ImageDisplay< ? > imageDisplay )
	{
		imageDisplay.imageVolumeViewer = new ImageVolumeViewer( imageDisplay.sourceAndConverters(), universeManager );
		Double[] resolution3dView = imageDisplay.getResolution3dView();
		if ( resolution3dView != null ) {
			imageDisplay.imageVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive( imageDisplay.getResolution3dView() ));
		}
		imageDisplay.imageVolumeViewer.showImages( imageDisplay.showImagesIn3d() );

		final Collection< ? extends SourceAndConverter< ? > > sourceAndConverters = imageDisplay.sourceAndConverters();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			sacService.setMetadata( sourceAndConverter, ImageVolumeViewer.class.getName(), imageDisplay.imageVolumeViewer );

	}

	private void initTableView( AbstractAnnotationDisplay< ? extends Annotation > display )
	{
		display.tableView = new TableView( display );
		display.selectionModel.listeners().add( display.tableView );
		display.coloringModel.listeners().add( display.tableView );

		// TODO: currently we must show the table here
		//   in order to instantiate the window.
		//   This window is needed in {@code UserInterfaceHelper}
		//   in the function {@code createWindowVisibilityCheckbox},
		//   in which the table window will be
		//   hidden, if {@code display.showTable == false}.
		//   It would be good if we would not have to show it.
		display.tableView.show();
		setTablePosition( display.sliceViewer.getWindow(), display.tableView.getWindow() );
	}

	private void setTablePosition( Window reference, Window table )
	{
		// set the table position.
		// the table may not visible at this point, but the window exists
		// already and thus its location can be set.
		// the table can be toggled visible in the UserInterfaceHelper.
		SwingUtilities.invokeLater( () -> WindowArrangementHelper.bottomAlignWindow( reference, table, true, true ) );
	}

	private void initSegmentVolumeViewer( SegmentationDisplay< ? extends AnnotatedSegment > display )
	{
		display.coloringModel.listeners().add( display.bigVolumeViewer );
		display.selectionModel.listeners().add( display.bigVolumeViewer );

		display.segmentVolumeViewer = new SegmentVolumeViewer( display.selectionModel, display.coloringModel, display.images(), universeManager );
		Double[] resolution3dView = display.getResolution3dView();
		if ( resolution3dView != null ) {
			display.segmentVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive( display.getResolution3dView() ) );
		}
		display.segmentVolumeViewer.showSegments( display.showSelectedSegmentsIn3d(), true );
		display.coloringModel.listeners().add( display.segmentVolumeViewer );
		display.selectionModel.listeners().add( display.segmentVolumeViewer );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public synchronized void removeDisplay( Display display, boolean closeImgLoader )
	{
		if ( display instanceof AbstractDisplay )
		{
			if ( ( ( AbstractDisplay ) display).bigVolumeViewer != null )
			{
				if ( ( ( AbstractDisplay ) display ).bigVolumeViewer.getBVV() != null )
				{
					( ( AbstractDisplay ) display ).bigVolumeViewer.removeSources( display.sourceAndConverters() );
				}
			}
		}
		
		if ( display instanceof AbstractAnnotationDisplay )
		{
			final AbstractAnnotationDisplay< ? > annotationDisplay = ( AbstractAnnotationDisplay< ? > ) display;
			annotationDisplay.sliceView.close( closeImgLoader );

			if ( annotationDisplay.tableView != null )
			{
				annotationDisplay.tableView.close();
				annotationDisplay.scatterPlotView.close();
				if ( annotationDisplay instanceof SegmentationDisplay )
					( ( SegmentationDisplay ) annotationDisplay ).segmentVolumeViewer.close();
			}

			if  ( annotationDisplay instanceof SegmentationDisplay )
			{
				( ( SegmentationDisplay ) annotationDisplay ).segmentVolumeViewer.close();
			}

		}
		else if ( display instanceof ImageDisplay )
		{
			final ImageDisplay imageDisplay = ( ImageDisplay ) display;
			imageDisplay.imageSliceView.close( false );
			imageDisplay.imageVolumeViewer.close();
		}
		
		userInterface.removeDisplaySettingsPanel( display );
		currentDisplays.remove( display );
	}

	// TODO: typing ( or remove )
	public Collection< AbstractAnnotationDisplay< ? > > getAnnotationDisplays()
	{
        return getCurrentSourceDisplays().stream()
				.filter( s -> s instanceof AbstractAnnotationDisplay )
				.map( s -> ( AbstractAnnotationDisplay< ? > ) s )
				.collect( Collectors.toList() );
	}

	public void close()
	{
		IJ.log( "Closing BDV..." );
		removeAllSourceDisplays( true );
		sliceViewer.getBdvHandle().close();
		IJ.log( "Closing 3D Viewer..." );
		universeManager.close();
		IJ.log ("Closing BVV...");
		bigVolumeViewer.close();
		IJ.log( "Closing UI..." );
		userInterface.close();
		// see also https://github.com/mobie/mobie-viewer-fiji/issues/857
		IJ.log( "Clearing SpimData cache..." );
		DataStore.clearSpimDataCache();
	}

	public BigVolumeViewerMoBIE getBigVolumeViewer()
	{
		return bigVolumeViewer;
	}
}
