package de.embl.cba.mobie.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.annotate.AnnotatedIntervalAdapter;
import de.embl.cba.mobie.annotate.AnnotatedIntervalTableRow;
import de.embl.cba.mobie.bdv.view.AnnotatedIntervalSliceView;
import de.embl.cba.mobie.color.MoBIEColoringModel;
import de.embl.cba.mobie.display.AnnotatedRegionDisplay;
import de.embl.cba.mobie.display.AnnotatedIntervalDisplay;
import de.embl.cba.mobie.playground.PlaygroundUtils;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.bdv.view.ImageSliceView;
import de.embl.cba.mobie.bdv.view.SegmentationSliceView;
import de.embl.cba.mobie.bdv.view.SliceViewer;
import de.embl.cba.mobie.plot.ScatterPlotViewer;
import de.embl.cba.mobie.segment.SegmentAdapter;
import de.embl.cba.mobie.display.ImageSourceDisplay;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.source.SegmentationSource;
import de.embl.cba.mobie.table.TableDataFormat;
import de.embl.cba.mobie.table.TableViewer;
import de.embl.cba.mobie.transform.*;
import de.embl.cba.mobie.ui.UserInterface;
import de.embl.cba.mobie.ui.WindowArrangementHelper;
import de.embl.cba.mobie.view.additionalviews.AdditionalViewsLoader;
import de.embl.cba.mobie.view.saving.ViewsSaver;
import de.embl.cba.mobie.volume.SegmentsVolumeViewer;
import de.embl.cba.mobie.volume.UniverseManager;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;


import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;
import static de.embl.cba.mobie.ui.UserInterfaceHelper.resetSystemSwingLookAndFeel;

public class ViewerManager
{
	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private ArrayList< SourceDisplay > sourceDisplays;
	private final BdvHandle bdvHandle;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewsSaver viewsSaver;

	public ViewerManager( MoBIE moBIE, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints );
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

	public void showInTableViewer( SegmentationSourceDisplay display  )
	{
		Map<String, String> sourceNameToTableDir = new HashMap<>();
		for ( String source: display.getSources() )
		{
			sourceNameToTableDir.put( source, moBIE.getTablesDirectoryPath( (SegmentationSource) moBIE.getSource( source ) )
			);
		}
		display.tableViewer = new TableViewer<>( moBIE, display.tableRows, display.selectionModel, display.coloringModel,
				display.getName(), sourceNameToTableDir, false ).show();
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	public ArrayList< SourceDisplay > getSourceDisplays()
	{
		return sourceDisplays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public ViewsSaver getViewsSaver() { return viewsSaver; }

	public View getCurrentView( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform ) {

		List< SourceDisplay > viewSourceDisplays = new ArrayList<>();
		List< SourceTransformer > viewSourceTransforms = new ArrayList<>();

		for ( SourceDisplay sourceDisplay : sourceDisplays )
		{
			SourceDisplay currentDisplay = null;

			if ( sourceDisplay instanceof ImageSourceDisplay )
			{
				currentDisplay = new ImageSourceDisplay( (ImageSourceDisplay) sourceDisplay );
			}
			else if ( sourceDisplay instanceof  SegmentationSourceDisplay )
			{
				SegmentationSourceDisplay segmentationSourceDisplay = (SegmentationSourceDisplay) sourceDisplay;
				if ( segmentationSourceDisplay.tableViewer.hasColumnsFromTablesOutsideProject() ) {
					IJ.log( "Cannot make a view with tables that have columns loaded from the filesystem (not within the project).");
					return null;
				}
				currentDisplay = new SegmentationSourceDisplay( segmentationSourceDisplay );
			}

			if ( currentDisplay != null )
			{
				viewSourceDisplays.add( currentDisplay );
			}

			// TODO - would be good to pick up any manual transforms here too. This would allow e.g. manual placement
			// of differing sized sources into a grid
			if ( sourceDisplay.sourceTransformers != null )
			{
				for ( SourceTransformer sourceTransformer: sourceDisplay.sourceTransformers )
				{
					if ( ! viewSourceTransforms.contains( sourceTransformer ) )
					{
						viewSourceTransforms.add( sourceTransformer );
					}
				}
			}
		}

		if ( includeViewerTransform )
		{
			AffineTransform3D normalisedViewTransform = Utils.createNormalisedViewerTransform( bdvHandle,
					PlaygroundUtils.getWindowCentreInPixelUnits( bdvHandle ) );

			final NormalizedAffineViewerTransform transform = new NormalizedAffineViewerTransform( normalisedViewTransform.getRowPackedCopy(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, transform, isExclusive);
		} else {
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, isExclusive);
		}
	}

	public synchronized void show( View view )
	{
		if ( view.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		setMoBIESwingLookAndFeel();

		// show the displays if there are any
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();
		if ( sourceDisplays != null )
		{
			for ( SourceDisplay sourceDisplay : sourceDisplays )
			{
				sourceDisplay.sourceTransformers = view.getSourceTransforms();
				showSourceDisplay( sourceDisplay );
			}
		}

		resetSystemSwingLookAndFeel();

		// adjust the viewer transform
		if ( view.getViewerTransform() != null )
		{
			ViewerTransformChanger.changeViewerTransform( bdvHandle, view.getViewerTransform() );
		}
		else
		{
			if ( view.isExclusive() || this.sourceDisplays.size() == 1 )
			{
				// focus on the image that was added last
				final SourceDisplay sourceDisplay = this.sourceDisplays.get( this.sourceDisplays.size() - 1 );
				new ViewerTransformAdjuster( bdvHandle, sourceDisplay.sourceAndConverters.get( 0 ) ).run();
			}
		}
	}

	private void showSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplays.contains( sourceDisplay ) ) return;

		sourceDisplay.sliceViewer = sliceViewer;

		if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			showImageDisplay( ( ImageSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			showSegmentationDisplay( ( SegmentationSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof AnnotatedIntervalDisplay )
		{
			showAnnotatedIntervalDisplay( ( AnnotatedIntervalDisplay ) sourceDisplay );
		}

		// register the SAC with MoBIE for access and closing
		sourceDisplay.sourceAndConverters.stream().forEach( sac -> moBIE.registerSourceAndConverter( sac.getSpimSource().getName(), sac)  );

		userInterface.addSourceDisplay( sourceDisplay );
		sourceDisplays.add( sourceDisplay );
	}

	private void removeAllSourceDisplays()
	{
		// create a copy of the currently shown displays...
		final ArrayList< SourceDisplay > currentSourceDisplays = new ArrayList<>( sourceDisplays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( SourceDisplay sourceDisplay : currentSourceDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			removeSourceDisplay( sourceDisplay );
		}
	}

	private void showImageDisplay( ImageSourceDisplay imageDisplay )
	{
		imageDisplay.imageSliceView = new ImageSliceView( moBIE, imageDisplay, bdvHandle );
	}

	// TODO: own class: SourceAnnotationDisplayConfigurator
	private void showAnnotatedIntervalDisplay( AnnotatedIntervalDisplay annotationDisplay )
	{
		annotationDisplay.tableRows = moBIE.loadAnnotatedIntervalTables( annotationDisplay );
		annotationDisplay.annotatedIntervalAdapter = new AnnotatedIntervalAdapter<>( annotationDisplay.tableRows );

		configureMoBIEColoringModel( annotationDisplay );
		annotationDisplay.selectionModel = new DefaultSelectionModel<>();
		annotationDisplay.coloringModel.setSelectionModel(  annotationDisplay.selectionModel );

		// set selected segments
		if ( annotationDisplay.getSelectedAnnotationIds() != null )
		{
			final List< AnnotatedIntervalTableRow > annotatedIntervals = annotationDisplay.annotatedIntervalAdapter.getAnnotatedIntervals( annotationDisplay.getSelectedAnnotationIds() );
			annotationDisplay.selectionModel.setSelected( annotatedIntervals, true );
		}

		showInSliceViewer( annotationDisplay );
		showInTableViewer( annotationDisplay );
		initScatterPlotViewer( annotationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			WindowArrangementHelper.bottomAlignWindow( annotationDisplay.sliceViewer.getWindow(), annotationDisplay.tableViewer.getWindow() );
		} );
	}

	private void showInTableViewer( AnnotatedIntervalDisplay annotationDisplay )
	{
		HashMap<String, String> nameToTableDir = new HashMap<>();
		nameToTableDir.put( annotationDisplay.getName(), annotationDisplay.getTableDataFolder( TableDataFormat.TabDelimitedFile ) );
		annotationDisplay.tableViewer = new TableViewer<>( moBIE, annotationDisplay.tableRows, annotationDisplay.selectionModel, annotationDisplay.coloringModel, annotationDisplay.getName(), nameToTableDir, true ).show();
	}

	// TODO: own class: SegmentationDisplayConfigurator
	private void showSegmentationDisplay( SegmentationSourceDisplay segmentationDisplay )
	{
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
			showInTableViewer( segmentationDisplay );
			initScatterPlotViewer( segmentationDisplay );

			SwingUtilities.invokeLater( () ->
			{
				WindowArrangementHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
			} );

			initVolumeViewer( segmentationDisplay );
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

	private void showInSliceViewer( AnnotatedIntervalDisplay annotatedIntervalDisplay )
	{
		annotatedIntervalDisplay.sliceView = new AnnotatedIntervalSliceView( moBIE, annotatedIntervalDisplay, bdvHandle );
	}

	private void initVolumeViewer( SegmentationSourceDisplay display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeViewer<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, universeManager );
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d() );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );

		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			sacService.setMetadata( sourceAndConverter, SegmentsVolumeViewer.VOLUME_VIEW, display.segmentsVolumeViewer  );
		}
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			final SegmentationSourceDisplay segmentationDisplay = ( SegmentationSourceDisplay ) sourceDisplay;
			segmentationDisplay.sliceView.close();
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
			imageDisplay.imageSliceView.close();
		}
		else if ( sourceDisplay instanceof AnnotatedIntervalDisplay )
		{
			// TODO: Code duplication (sourceDisplay instanceof SegmentationSourceDisplay)
			final AnnotatedIntervalDisplay annotatedIntervalDisplay = ( AnnotatedIntervalDisplay ) sourceDisplay;
			annotatedIntervalDisplay.sliceView.close();
			annotatedIntervalDisplay.tableViewer.close();
			annotatedIntervalDisplay.scatterPlotViewer.close();
		}

		userInterface.removeDisplaySettingsPanel( sourceDisplay );
		sourceDisplays.remove( sourceDisplay );
	}

	public Collection< SegmentationSourceDisplay > getSegmentationDisplays()
	{
		final List< SegmentationSourceDisplay > segmentationDisplays = getSourceDisplays().stream().filter( s -> s instanceof SegmentationSourceDisplay ).map( s -> ( SegmentationSourceDisplay ) s ).collect( Collectors.toList() );

		return segmentationDisplays;
	}

	public void close()
	{
		removeAllSourceDisplays();
		sliceViewer.getBdvHandle().close();
		universeManager.close();
	}
}
