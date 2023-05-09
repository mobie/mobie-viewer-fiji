/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.ImageNameOverlay;
import org.embl.mobie.lib.bdv.view.AnnotationSliceView;
import org.embl.mobie.lib.bdv.view.ImageSliceView;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.color.NumericAnnotationColoringModel;
import org.embl.mobie.lib.color.lut.ColumnARGBLut;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.image.StitchedAnnotatedLabelImage;
import org.embl.mobie.lib.image.StitchedImage;
import org.embl.mobie.lib.plot.ScatterPlotSettings;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.select.MoBIESelectionModel;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.CropTransformation;
import org.embl.mobie.lib.serialize.transformation.GridTransformation;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.serialize.transformation.TimepointsTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.image.CroppedImage;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.table.ConcatenatedAnnotationTableModel;
import org.embl.mobie.lib.table.RegionDisplayAnnDataCreator;
import org.embl.mobie.lib.table.TableView;
import org.embl.mobie.lib.transform.viewer.ImageZoomViewerTransform;
import org.embl.mobie.lib.transform.viewer.MoBIEViewerTransformAdjuster;
import org.embl.mobie.lib.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;
import org.embl.mobie.lib.transform.viewer.ViewerTransformChanger;
import org.embl.mobie.lib.transform.TransformHelper;
import org.embl.mobie.lib.transform.image.ImageTransformer;
import org.embl.mobie.lib.ui.UserInterface;
import org.embl.mobie.lib.ui.WindowArrangementHelper;
import org.embl.mobie.lib.view.save.ViewSaver;
import org.embl.mobie.lib.volume.ImageVolumeViewer;
import org.embl.mobie.lib.volume.SegmentVolumeViewer;
import org.embl.mobie.lib.volume.UniverseManager;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
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
	private List< Transformation > currentTransformations;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;

	public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentDisplays = new ArrayList<>();
		currentTransformations = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D );
		universeManager = new UniverseManager();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

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

	private void addManualTransforms( List< Transformation > viewSourceTransforms, List< ? extends SourceAndConverter< ? >> sourceAndConverters )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			Source< ? > source = sourceAndConverter.getSpimSource();

			final TransformedSource< ? > transformedSource = SourceHelper.unwrapSource( source, TransformedSource.class );

			if ( transformedSource != null )
			{
				AffineTransform3D fixedTransform = new AffineTransform3D();
				transformedSource.getFixedTransform( fixedTransform );
				if ( ! fixedTransform.isIdentity() )
				{
					List< String > sources = new ArrayList<>();
					sources.add( source.getName() );
					viewSourceTransforms.add( new AffineTransformation( "manualTransform", fixedTransform.getRowPackedCopy(), sources ) );
				}
			}
        }
    }

	public View createViewFromCurrentState( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform )
	{
		List< Transformation > viewTransformations = new ArrayList<>( currentTransformations );

		// Create copies of the current displays where the
		// serializable fields are properly set
		List< Display< ? > > viewDisplays = new ArrayList<>();
		for ( Display< ? > display : currentDisplays )
		{
			if ( display instanceof ImageDisplay )
				viewDisplays.add( new ImageDisplay( ( ImageDisplay ) display ) );
			else if ( display instanceof SegmentationDisplay )
				viewDisplays.add( new SegmentationDisplay( ( SegmentationDisplay ) display ) );
			else if ( display instanceof RegionDisplay )
				viewDisplays.add( new RegionDisplay( ( RegionDisplay ) display ) );
			else if ( display instanceof SpotDisplay )
				viewDisplays.add( new SpotDisplay( ( SpotDisplay) display ) );
			else
				throw new UnsupportedOperationException( "Serialisation of a " + display.getClass().getName() + " is not yet supported." );

			// Add transforms that were done by the user in BDV,
			// using, e.g., the manual transform or BigWarp.
			// Note that other transforms are already captured
			// in the {@code viewTransformations} list.
			addManualTransforms( viewTransformations, display.sourceAndConverters() );
		}

		if ( includeViewerTransform )
		{
			final BdvHandle bdvHandle = sliceViewer.getBdvHandle();
			AffineTransform3D normalisedViewTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
			final NormalizedAffineViewerTransform transform = new NormalizedAffineViewerTransform( normalisedViewTransform.getRowPackedCopy(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
			return new View( "", uiSelectionGroup, viewDisplays, viewTransformations, transform, isExclusive);
		}
		else
		{
			return new View( "", uiSelectionGroup, viewDisplays, viewTransformations, isExclusive );
		}
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
		IJ.log( "Opening view: " + view.getName() );

		if ( view.isExclusive() )
		{
			removeAllSourceDisplays( true );
			DataStore.clearImages();
		}

		final boolean viewerWasEmpty = currentDisplays.size() == 0;

		// init and transform the data of this view
		// currently, data is reloaded every time a view is shown
		// this is a bit expensive, but simpler and has the
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
				final AffineTransform3D transform = TransformHelper.getIntervalViewerTransform( bdvHandle, mask );
				ViewerTransformChanger.changeLocation( bdvHandle, transform, 0 );
			}
			else
			{
				ViewerTransformChanger.changeLocation( bdvHandle, viewerTransform );
			}
		}

		// display the data
		final List< Display< ? > > displays = view.displays();
		for ( Display< ? > display : displays )
			show( display );

		// adjust viewer transform to accommodate the displayed sources.
		// note that if {@code view.getViewerTransform() != null}
		// the viewer transform has already been adjusted above.
		if ( view.getViewerTransform() == null && currentDisplays.size() > 0 && viewerWasEmpty )
		{
			new MoBIEViewerTransformAdjuster(
					sliceViewer.getBdvHandle(),
					currentDisplays.get( 0 ) ).applyMultiSourceTransform();
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

		IJ.log("Opened view: " + view.getName() + " in " + (System.currentTimeMillis() - startTime) + " ms." );
	}

	// initialize and transform
	public void initData( View view )
	{
		// fetch names of all data sources that are
		// either to be shown or to be transformed
		final Map< String, Object > sourceToTransformOrDisplay = view.getSources();
		if ( sourceToTransformOrDisplay.size() == 0 ) return;

		// instantiate source that can be directly opened
		// (other sources may be created later,
		// by a display or transformation)
		final List< DataSource > dataSources = moBIE.getDataSources( sourceToTransformOrDisplay.keySet() );

		for ( DataSource dataSource : dataSources )
		{
			final Object transformOrDisplay = sourceToTransformOrDisplay.get( dataSource.getName() );
			if ( transformOrDisplay instanceof MergedGridTransformation )
			{
				final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformOrDisplay;
				if ( mergedGridTransformation.metadataSource != null )
				{
					if ( dataSource.getName().equals( mergedGridTransformation.metadataSource ) )
						dataSource.preInit( true );
					else
						dataSource.preInit( false );
				}
				else // no metadata source specified, use the first in the grid as metadata source
				{
					final String firstImageInGrid = mergedGridTransformation.getSources().get( 0 );
					if ( dataSource.getName().equals( firstImageInGrid ) )
						dataSource.preInit( true );
					else
						dataSource.preInit( false );
				}
			}
			else
			{
				dataSource.preInit( true );
			}
		}

		moBIE.initDataSources( dataSources );

		// transform images
		// this may create new images with new names

		// TODO factor this out int an image transformer class
		final List< Transformation > transformations = view.getTransformations();
		if ( transformations != null )
		{
			for ( Transformation transformation : transformations )
			{
				currentTransformations.add( transformation );

				if ( transformation instanceof AffineTransformation )
				{
					final AffineTransformation< ? > affineTransformation = ( AffineTransformation< ? > ) transformation;

					final Set< Image< ? > > images = DataStore.getImageSet( transformation.getSources() );

					for ( Image< ? > image : images )
					{
						final Image< ? > transformedImage =
							ImageTransformer.applyAffineTransform(
									image,
									affineTransformation.getAffineTransform3D(),
									affineTransformation.getTransformedImageName( image.getName() ) );
						DataStore.putImage( transformedImage );
					}
				}
				else if ( transformation instanceof CropTransformation )
				{
					final CropTransformation< ? > cropTransformation = ( CropTransformation< ? > ) transformation;
					final List< String > targetImageNames = transformation.getSources();
					for ( String imageName : targetImageNames )
					{
						final CroppedImage< ? > croppedImage = new CroppedImage<>(
								DataStore.getImage( imageName ),
								cropTransformation.getTransformedImageName( imageName ),
								cropTransformation.min,
								cropTransformation.max,
								cropTransformation.centerAtOrigin );
						DataStore.putImage( croppedImage );
					}
				}
				else if ( transformation instanceof MergedGridTransformation )
				{
					final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformation;
					final List< String > targetImageNames = transformation.getSources();
					final List< ? extends Image< ? > > gridImages = DataStore.getImageList( targetImageNames );

					// Fetch grid metadata image
					Image< ? > metadataImage = ( mergedGridTransformation.metadataSource == null ) ? gridImages.get( 0 ) : DataStore.getImage( mergedGridTransformation.metadataSource );

					// Create the stitched grid image
					//
					if ( gridImages.get( 0 ) instanceof AnnotatedLabelImage )
					{
						final StitchedAnnotatedLabelImage< ? extends Annotation > annotatedStitchedImage = new StitchedAnnotatedLabelImage( gridImages, metadataImage, mergedGridTransformation.positions, mergedGridTransformation.getName(), mergedGridTransformation.margin );

						if ( ! mergedGridTransformation.lazyLoadTables )
						{
							// force loading of all tables
							//
							final ConcatenatedAnnotationTableModel< ? extends Annotation > concatenatedTableModel = ( ConcatenatedAnnotationTableModel ) annotatedStitchedImage.getAnnData().getTable();
							final Set< ? extends AnnotationTableModel< ? extends Annotation > > tableModels = concatenatedTableModel.getTableModels();
							for ( AnnotationTableModel< ? extends Annotation > tableModel : tableModels )
							{
								tableModel.annotations();
							}
						}

						DataStore.putImage( annotatedStitchedImage );
					}
					else
					{
						final StitchedImage stitchedImage = new StitchedImage( gridImages, metadataImage, mergedGridTransformation.positions, mergedGridTransformation.getName(), mergedGridTransformation.margin );
						DataStore.putImage( stitchedImage );
					}
				}
				else if ( transformation instanceof GridTransformation )
				{
					final GridTransformation gridTransformation = ( GridTransformation ) transformation;

					final List< List< String > > nestedSources = gridTransformation.nestedSources;
					final List< List< ? extends Image< ? > > > nestedImages = new ArrayList<>();
					for ( List< String > sources : nestedSources )
					{
						final List< ? extends Image< ? > > images = DataStore.getImageList( sources );
						nestedImages.add( images );
					}

					// The size of the tile of the grid is the size of the
					// largest union mask of the images at
					// the grid positions.
					double[] tileRealDimensions = new double[ 2 ];
					for ( List< ? extends Image< ? > > images : nestedImages )
					{
						final RealMaskRealInterval unionMask = TransformHelper.getUnionMask( images );
						final double[] realDimensions = TransformHelper.getRealDimensions( unionMask );
						for ( int d = 0; d < 2; d++ )
							tileRealDimensions[ d ] = realDimensions[ d ] > tileRealDimensions[ d ] ? realDimensions[ d ] : tileRealDimensions[ d ];
					}

					// Add a margin to the tiles
					for ( int d = 0; d < 2; d++ )
						tileRealDimensions[ d ] = tileRealDimensions[ d ] * ( 1.0 + 2 * gridTransformation.margin );

					// Compute the corresponding offset of where to place
					// the images within the tile
					final double[] offset = new double[ 2 ];
					for ( int d = 0; d < 2; d++ )
						offset[ d ] = tileRealDimensions[ d ] * gridTransformation.margin;

					final List< int[] > gridPositions = gridTransformation.positions == null ? TransformHelper.createGridPositions( nestedSources.size() ) : gridTransformation.positions;

					final List< ? extends Image< ? > > transformedImages = ImageTransformer.gridTransform( nestedImages, gridTransformation.transformedNames, gridPositions, tileRealDimensions, gridTransformation.centerAtOrigin, offset );

					DataStore.putImages( transformedImages );
				}
				else if ( transformation instanceof TimepointsTransformation )
				{
					final TimepointsTransformation< ? > timepointsTransformation = ( TimepointsTransformation ) transformation;

					final Set< Image< ? > > images = DataStore.getImageSet( timepointsTransformation.getSources() );

					for ( Image< ? > image : images )
					{
						final Image< ? > transformedImage =
								ImageTransformer.applyTimepointsTransform(
										image,
										timepointsTransformation.getTimepointsMapping(),
										timepointsTransformation.isKeep(),
										timepointsTransformation.getTransformedImageName( image.getName() ) );
						DataStore.putImage( transformedImage );
					}
				}
				else
				{
					throw new UnsupportedOperationException( "Transformations of type " + transformation.getClass().getName() + " are not yet implemented.");
				}
			}
		}

		// instantiate {@code RegionDisplay}
		// note that this cannot be done already in MoBIE.initData()
		// because we need to wait until all images are initialised
		for ( Display< ? > display : view.displays() )
		{
			// https://github.com/mobie/mobie-viewer-fiji/issues/818
			if ( display instanceof RegionDisplay )
			{
				// Note that the imageNames that are referred
				// to here must exist in this view.
				// Thus the {@code RegionDisplay} must be build
				// *after* the above transformations,
				// which may create new images
				// that could be referred to here.
				final RegionDisplay< ? > regionDisplay = ( RegionDisplay< ? > ) display;
				final RegionDisplayAnnDataCreator annDataCreator = new RegionDisplayAnnDataCreator( moBIE, regionDisplay );
				AnnData< AnnotatedRegion > annData = annDataCreator.getAnnData();
				final RegionAnnotationImage< AnnotatedRegion > regionAnnotationImage = new RegionAnnotationImage( regionDisplay.getName(), annData, regionDisplay.timepoints() );
				DataStore.putImage( regionAnnotationImage );
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

		if ( display instanceof ImageDisplay )
		{
			for ( String name : display.getSources() )
				display.images().add( ( Image ) DataStore.getImage( name ) );
			showImageDisplay( ( ImageDisplay ) display );
		}
		else if ( display instanceof AbstractAnnotationDisplay )
		{
			final AbstractAnnotationDisplay< A > annotationDisplay = ( AbstractAnnotationDisplay ) display;

			// create combined AnnData from all images
			// that are shown
			for ( String name : display.getSources() )
			{
				final Image< ? > image = DataStore.getImage( name );
				annotationDisplay.images().add( ( Image< AnnotationType< A > > ) image );
			}
			annotationDisplay.initAnnData();

			// load additional tables (to be merged)
			final List< String > requestedTableChunks = annotationDisplay.getRequestedTableChunks();
			if ( requestedTableChunks != null )
				for ( String tableChunk : requestedTableChunks )
				{
					final AnnotationTableModel< A > tableModel = annotationDisplay.getAnnData().getTable();
					tableModel.loadTableChunk( tableChunk );
				}

			// configure selection model
			//
			annotationDisplay.selectionModel = new MoBIESelectionModel<>();

			// set selected segments
			//
			final Set< String > selectedAnnotationIds = annotationDisplay.selectedAnnotationIds();
			if ( selectedAnnotationIds != null )
			{
				final Set< A > annotations = annotationDisplay.annotationAdapter().getAnnotations( selectedAnnotationIds );
				annotationDisplay.selectionModel.setSelected( annotations, true );
			}

			// configure coloring model
			//
			String lut = annotationDisplay.getLut();

			if ( LUTs.isCategorical( lut ) )
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

				annotationDisplay.coloringModel = new MobieColoringModel( coloringModel, annotationDisplay.selectionModel, annotationDisplay.getSelectionColor(), annotationDisplay.getOpacityNotSelected() );
			}
			else if ( LUTs.isNumeric( lut ) )
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
			else
			{
				throw new UnsupportedOperationException("Coloring LUT " + lut + " is not supported.");
			}

			// show in slice viewer
			//
			annotationDisplay.sliceViewer = sliceViewer;
			annotationDisplay.sliceView = new AnnotationSliceView<>( moBIE, annotationDisplay );
			initTableView( annotationDisplay );
			initScatterPlotView( annotationDisplay );
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

	public void showImageDisplay( ImageDisplay imageDisplay )
	{
		imageDisplay.sliceViewer = sliceViewer;
		imageDisplay.imageSliceView = new ImageSliceView( moBIE, imageDisplay );
		initImageVolumeViewer( imageDisplay );
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
		// Note that currently we must show the table here
		// in order to instantiate the window.
		// This window is needed in {@code UserInterfaceHelper}
		// in the function {@code createWindowVisibilityCheckbox},
		// in which the table window will be
		// hidden, if {@code display.showTable == false}.
		display.tableView.show();
		setTablePosition( display.sliceViewer.getWindow(), display.tableView.getWindow() );
		display.selectionModel.listeners().add( display.tableView );
		display.coloringModel.listeners().add( display.tableView );
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
		display.segmentVolumeViewer = new SegmentVolumeViewer( display.selectionModel, display.coloringModel, display.images(), universeManager );
		Double[] resolution3dView = display.getResolution3dView();
		if ( resolution3dView != null ) {
			display.segmentVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive( display.getResolution3dView() ) );
		}
		display.segmentVolumeViewer.showSegments( display.showSelectedSegmentsIn3d(), true );
		display.coloringModel.listeners().add( display.segmentVolumeViewer );
		display.selectionModel.listeners().add( display.segmentVolumeViewer );
	}

	public synchronized void removeDisplay( Display display, boolean closeImgLoader )
	{
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

		updateCurrentSourceTransformers();
	}

	private void updateCurrentSourceTransformers()
	{
		// remove any sourceTransformers, where none of its relevant sources are displayed

		// create a copy of the currently shown source transformers, so we don't iterate over a list that we modify
		final ArrayList< Transformation > imageTransformersCopy = new ArrayList<>( this.currentTransformations ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( Display display: currentDisplays )
			currentlyDisplayedSources.addAll( display.getSources() );

		for ( Transformation imageTransformation : imageTransformersCopy )
		{
			if ( ! currentlyDisplayedSources.stream().anyMatch( s -> imageTransformation.getSources().contains( s ) ) )
				currentTransformations.remove( imageTransformation );
		}
	}

	// TODO: typing ( or remove )
	public Collection< AbstractAnnotationDisplay< ? > > getAnnotationDisplays()
	{
		final List< AbstractAnnotationDisplay< ? > > collect = getCurrentSourceDisplays().stream().filter( s -> s instanceof AbstractAnnotationDisplay ).map( s -> ( AbstractAnnotationDisplay< ? > ) s ).collect( Collectors.toList() );

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
