package org.embl.mobie.viewer.view;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.annotate.AnnotatedMaskAdapter;
import org.embl.mobie.viewer.annotate.AnnotatedMaskTableRow;
import org.embl.mobie.viewer.bdv.view.AnnotatedMaskSliceView;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.bdv.view.SegmentationSliceView;
import org.embl.mobie.viewer.bdv.view.SliceViewer;
import org.embl.mobie.viewer.color.MoBIEColoringModel;
import org.embl.mobie.viewer.display.*;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.segment.SegmentAdapter;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SegmentationSource;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.TableViewer;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformChanger;
import org.embl.mobie.viewer.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.viewer.transform.SourceTransformer;
import org.embl.mobie.viewer.transform.TransformHelpers;
import org.embl.mobie.viewer.ui.MoBIELookAndFeelToggler;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViewsLoader;
import org.embl.mobie.viewer.view.saving.ViewsSaver;
import org.embl.mobie.viewer.volume.ImageVolumeViewer;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import org.embl.mobie.viewer.volume.UniverseManager;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.embl.mobie.viewer.MoBIEHelper.containsAtLeastOne;

public class ViewManager
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private List<SourceDisplay> currentSourceDisplays;
	private List<SourceTransformer> currentSourceTransformers;
	private final BdvHandle bdvHandle;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewsSaver viewsSaver;

    public List<SourceTransformer> getCurrentSourceTransformers()
    {
        return currentSourceTransformers;
    }

    public UserInterface getUserInterface()
    {
        return userInterface;
    }

    public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentSourceDisplays = new ArrayList<>();
		currentSourceTransformers = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints, moBIE.getProjectCommands() );
		universeManager = new UniverseManager();
		bdvHandle = sliceViewer.get();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewsSaver = new ViewsSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

	public static void initScatterPlotViewer( AnnotatedRegionDisplay< ? > display )
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

	private static void configureMoBIEColoringModel( AnnotatedRegionDisplay< ? > display )
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

			display.coloringModel = new MoBIEColoringModel( coloringModel );
		}
		else
		{
			display.coloringModel = new MoBIEColoringModel<>( display.getLut() );
		}
	}

	public void initTableViewer( SegmentationSourceDisplay display  )
	{
		Map<String, String> sourceNameToTableDir = new HashMap<>();
		for ( String source: display.getSources() )
		{
			try
			{
				sourceNameToTableDir.put( source, moBIE.getTablesDirectoryPath( ( SegmentationSource ) moBIE.getSource( source ) )
				);
			}
			catch ( Exception e )
			{
				System.out.println("[WARNING] Could not store table directory for " + source );
				sourceNameToTableDir.put( source, null );
			}
		}
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

	public ViewsSaver getViewsSaver() { return viewsSaver; }

	private boolean hasColumnsOutsideProject( AnnotatedRegionDisplay annotatedRegionDisplay ) {
		if ( annotatedRegionDisplay.tableViewer.hasColumnsFromTablesOutsideProject() )
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

			if ( sourceDisplay instanceof ImageSourceDisplay)
			{
				ImageSourceDisplay imageSourceDisplay = ( ImageSourceDisplay ) sourceDisplay;
				currentDisplay = new ImageSourceDisplay( imageSourceDisplay );
				addManualTransforms( viewSourceTransforms, imageSourceDisplay.sourceNameToSourceAndConverter );
			} else if ( sourceDisplay instanceof SegmentationSourceDisplay )
			{
				SegmentationSourceDisplay segmentationSourceDisplay = ( SegmentationSourceDisplay ) sourceDisplay;
				if ( hasColumnsOutsideProject( segmentationSourceDisplay ) ) { return null; }
				currentDisplay = new SegmentationSourceDisplay( segmentationSourceDisplay );
				addManualTransforms( viewSourceTransforms, segmentationSourceDisplay.sourceNameToSourceAndConverter );
			} else if ( sourceDisplay instanceof AnnotatedMaskDisplay )
			{
				AnnotatedMaskDisplay annotatedMaskDisplay = ( AnnotatedMaskDisplay ) sourceDisplay;
				if ( hasColumnsOutsideProject( annotatedMaskDisplay ) ) { return null; }
				currentDisplay = new AnnotatedMaskDisplay( annotatedMaskDisplay );
			}

			if ( currentDisplay != null )
			{
				viewSourceDisplays.add( currentDisplay );
			}
		}

		if ( includeViewerTransform )
		{
			AffineTransform3D normalisedViewTransform = TransformHelpers.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );

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
			removeAllSourceDisplays();
		}

		openAndTransformSources( view );

		// show the displays
		MoBIELookAndFeelToggler.setMoBIELaf();
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();
		for ( SourceDisplay sourceDisplay : sourceDisplays )
			showSourceDisplay( sourceDisplay );
		MoBIELookAndFeelToggler.resetMoBIELaf();

		// adjust viewer transform
		adjustViewerTransform( view );
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

		// wrap all in a final transformed source. This is so any manual transformations can be
		// retrieved separate from any from sourceTransformers.
		for ( String sourceName : sourceNameToSourceAndConverters.keySet() ) {
			SourceAndConverter<?> sourceAndConverter = new SourceAffineTransformer( sourceNameToSourceAndConverters.get( sourceName ), new AffineTransform3D()).getSourceOut();
			sourceNameToSourceAndConverters.put( sourceName, sourceAndConverter );
		}

		// register all available (transformed) sources in MoBIE
		// this is where the source and segmentation displays will
		// get the sources from
		moBIE.addTransformedSourceAndConverters( sourceNameToSourceAndConverters );
	}

	public void adjustViewerTransform( View view )
	{
		if ( view.getViewerTransform() != null )
		{
			MoBIEViewerTransformChanger.changeViewerTransform( bdvHandle, view.getViewerTransform() );
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

		if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			showImageDisplay( ( ImageSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			showSegmentationDisplay( ( SegmentationSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof AnnotatedMaskDisplay )
		{
			showAnnotatedMaskDisplay( ( AnnotatedMaskDisplay ) sourceDisplay );
		}

		userInterface.addSourceDisplay( sourceDisplay );
		currentSourceDisplays.add( sourceDisplay );
	}

	public synchronized void removeAllSourceDisplays()
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
			removeSourceDisplay( sourceDisplay, true );
		}
	}

	private void showImageDisplay( ImageSourceDisplay imageDisplay )
	{
		imageDisplay.sliceViewer = sliceViewer;
		imageDisplay.imageSliceView = new ImageSliceView( moBIE, imageDisplay, bdvHandle );
		initImageVolumeViewer( imageDisplay );
	}

	// compare with initSegmentationVolumeViewer
	private void initImageVolumeViewer( ImageSourceDisplay imageDisplay )
	{
		imageDisplay.imageVolumeViewer = new ImageVolumeViewer( imageDisplay.sourceNameToSourceAndConverter, universeManager );
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceNameToSourceAndConverter.values() )
		{
			sacService.setMetadata( sourceAndConverter, ImageVolumeViewer.class.getName(), imageDisplay.imageVolumeViewer );
		}
	}

	private void showAnnotatedMaskDisplay( AnnotatedMaskDisplay annotationDisplay )
	{
		annotationDisplay.sliceViewer = sliceViewer;
		annotationDisplay.tableRows = moBIE.loadAnnotatedMaskTables( annotationDisplay );
		annotationDisplay.annotatedMaskAdapter = new AnnotatedMaskAdapter( annotationDisplay.tableRows );

		configureMoBIEColoringModel( annotationDisplay );
		annotationDisplay.selectionModel = new DefaultSelectionModel<>();
		annotationDisplay.coloringModel.setSelectionModel( annotationDisplay.selectionModel );

		// set selected segments
		if ( annotationDisplay.getSelectedAnnotationIds() != null )
		{
			final List< AnnotatedMaskTableRow > annotatedMasks = annotationDisplay.annotatedMaskAdapter.getAnnotatedMasks( annotationDisplay.getSelectedAnnotationIds() );
			annotationDisplay.selectionModel.setSelected( annotatedMasks, true );
		}

		showInSliceViewer( annotationDisplay );
		initTableViewer( annotationDisplay );
		initScatterPlotViewer( annotationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			WindowArrangementHelper.bottomAlignWindow( annotationDisplay.sliceViewer.getWindow(), annotationDisplay.tableViewer.getWindow() );
		} );
	}

	private void initTableViewer( AnnotatedMaskDisplay display )
	{
		HashMap<String, String> nameToTableDir = new HashMap<>();
		nameToTableDir.put( display.getName(), display.getTableDataFolder( TableDataFormat.TabDelimitedFile ) );
		display.tableViewer = new TableViewer<>( moBIE, display.tableRows, display.selectionModel, display.coloringModel, display.getName(), nameToTableDir, true ).show();
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	private void showSegmentationDisplay( SegmentationSourceDisplay segmentationDisplay )
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

		configureMoBIEColoringModel( segmentationDisplay );
		segmentationDisplay.selectionModel = new DefaultSelectionModel<>();
		segmentationDisplay.coloringModel.setSelectionModel(  segmentationDisplay.selectionModel );

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

	private void loadTablesAndCreateImageSegments( SegmentationSourceDisplay segmentationDisplay )
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

	private void showInSliceViewer( SegmentationSourceDisplay segmentationDisplay )
	{
		segmentationDisplay.sliceView = new SegmentationSliceView<>( moBIE, segmentationDisplay, bdvHandle );
	}

	private void showInSliceViewer( AnnotatedMaskDisplay annotatedMaskDisplay )
	{
		annotatedMaskDisplay.sliceView = new AnnotatedMaskSliceView( moBIE, annotatedMaskDisplay, bdvHandle );
	}

	private void initSegmentationVolumeViewer( SegmentationSourceDisplay segmentationDisplay )
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
		if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			final SegmentationSourceDisplay segmentationDisplay = ( SegmentationSourceDisplay ) sourceDisplay;
			segmentationDisplay.sliceView.close( closeImgLoader );
			if ( segmentationDisplay.tableRows != null )
			{
				segmentationDisplay.tableViewer.close();
				segmentationDisplay.scatterPlotViewer.close();
				segmentationDisplay.segmentsVolumeViewer.close();
			}
		}
		else if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			final ImageSourceDisplay imageDisplay = ( ImageSourceDisplay ) sourceDisplay;
			imageDisplay.imageSliceView.close( false );
		}
		else if ( sourceDisplay instanceof AnnotatedMaskDisplay )
		{
			// TODO: Code duplication (sourceDisplay instanceof SegmentationSourceDisplay)
			final AnnotatedMaskDisplay annotatedMaskDisplay = ( AnnotatedMaskDisplay ) sourceDisplay;
			annotatedMaskDisplay.sliceView.close( false );
			annotatedMaskDisplay.tableViewer.close();
			annotatedMaskDisplay.scatterPlotViewer.close();
		}

		userInterface.removeDisplaySettingsPanel( sourceDisplay );
		currentSourceDisplays.remove( sourceDisplay );

		// remove any sourceTransformers, where none of its relevant 'sources' are displayed

		// create a copy of the currently shown source transformers, so we don't iterate over a list that we modify
		final ArrayList< SourceTransformer > sourceTransformersCopy = new ArrayList<>( this.currentSourceTransformers ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( SourceDisplay display: currentSourceDisplays ) {
			currentlyDisplayedSources.addAll( display.getSources() );
		}

		for ( SourceTransformer sourceTransformer: sourceTransformersCopy ) {
			if ( !containsAtLeastOne( currentlyDisplayedSources, sourceTransformer.getSources() )) {
				currentSourceTransformers.remove( sourceTransformer );
			}
		}
	}

	public Collection< AnnotatedRegionDisplay > getAnnotatedRegionDisplays()
	{
		final List< AnnotatedRegionDisplay > displays = getCurrentSourceDisplays().stream().filter( s -> s instanceof AnnotatedRegionDisplay ).map( s -> ( AnnotatedRegionDisplay ) s ).collect( Collectors.toList() );

		return displays;
	}

	public void close()
	{
		removeAllSourceDisplays();
		sliceViewer.getBdvHandle().close();
		universeManager.close();
	}
}
