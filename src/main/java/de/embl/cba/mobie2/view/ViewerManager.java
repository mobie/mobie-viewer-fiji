package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.bdv.ImageSliceView;
import de.embl.cba.mobie2.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie2.bdv.SliceViewer;
import de.embl.cba.mobie2.color.ColoringModelHelper;
import de.embl.cba.mobie2.grid.GridOverlayDisplay;
import de.embl.cba.mobie2.plot.ScatterPlotViewer;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.table.TableViewer;
import de.embl.cba.mobie2.transform.BdvLocationChanger;
import de.embl.cba.mobie2.transform.GridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.mobie2.ui.WindowArrangementHelper;
import de.embl.cba.mobie2.volume.SegmentsVolumeView;
import de.embl.cba.mobie2.volume.UniverseManager;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;


import javax.activation.UnsupportedDataTypeException;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.resetSystemSwingLookAndFeel;

public class ViewerManager
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private ArrayList< Display > displays;
	private final BdvHandle bdvHandle;
	private GridOverlayDisplay gridOverlayDisplay;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;

	public ViewerManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		displays = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints );
		universeManager = new UniverseManager();
		bdvHandle = sliceViewer.get();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE2 );
	}

	public static void initScatterPlotViewer( SegmentationDisplay display )
	{
		display.scatterPlotViewer = new ScatterPlotViewer<>( display.segments, display.selectionModel, display.coloringModel, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );

		if ( display.showScatterPlot() )
			display.scatterPlotViewer.show();
	}

	public static void initTableViewer( SegmentationDisplay display  )
	{
		display.tableViewer = new TableViewer<>( display.segments, display.selectionModel, display.coloringModel, display.getName() ).show();
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	public ArrayList< Display > getSourceDisplays()
	{
		return displays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public synchronized void show(View view )
	{
		if ( view.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		setMoBIESwingLookAndFeel();

		// fetch the source transformers
		List< SourceTransformer > sourceTransformers = null;
		if ( view.getSourceTransforms() != null )
			sourceTransformers = view.getSourceTransforms().stream().map( s -> s.get() ).collect( Collectors.toList() );

		// show the displays
		final List< SourceDisplaySupplier > sourceDisplays = view.getSourceDisplays();
		if ( sourceDisplays != null )
		{
			for ( SourceDisplaySupplier displaySupplier : sourceDisplays )
			{
				final Display display = displaySupplier.get();
				display.sourceTransformers = sourceTransformers;
				showSourceDisplay( display );
			}
		}

		createAndShowGridView( SwingUtilities.getWindowAncestor( sliceViewer.get().getViewerPanel() ), sourceTransformers );

		resetSystemSwingLookAndFeel();

		// adjust the viewer transform
		if ( view.getViewerTransform() != null )
		{
			BdvLocationChanger.moveToLocation( bdvHandle, view.getViewerTransform().get() );
		}
		else
		{
			if ( view.isExclusive() || displays.size() == 1 )
			{
				// focus on the image that was added last
				final Display display = displays.get( displays.size() - 1 );
				new ViewerTransformAdjuster( bdvHandle, display.sourceAndConverters.get( 0 ) ).run();
			}
		}
	}

	private void showSourceDisplay( Display display )
	{
		if ( displays.contains( display ) ) return;

		display.sliceViewer = sliceViewer;

		if ( display instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) display );
		}
		else if ( display instanceof SegmentationDisplay )
		{
			final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) display;
			showSegmentationDisplay( segmentationDisplay );
		}

		userInterface.addSourceDisplay( display );
		displays.add( display );
	}

	private void createAndShowGridView( Window window, List< SourceTransformer > sourceTransformers )
	{
		int i = 0; // TODO: can there be more than one?

		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				if ( sourceTransformer instanceof GridSourceTransformer )
				{
					final String tableDataLocation = ( ( GridSourceTransformer ) sourceTransformer ).getTableDataLocation();

					if ( tableDataLocation != null )
					{
						gridOverlayDisplay = new GridOverlayDisplay( moBIE2, bdvHandle,  "grid-" + (i++), tableDataLocation, ( GridSourceTransformer ) sourceTransformer );

						userInterface.addGridView( gridOverlayDisplay );
						displays.add( gridOverlayDisplay );

						SwingUtilities.invokeLater( () ->
						{
							WindowArrangementHelper.bottomAlignWindow( window, gridOverlayDisplay.getTableViewer().getWindow() );
						} );
					}
				}
			}
		}
	}

	private void removeAllSourceDisplays()
	{
		// create a copy of the currently shown displays...
		final ArrayList< Display > currentDisplays = new ArrayList<>( displays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( Display display : currentDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			removeSourceDisplay( display );
		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay )
	{
		imageDisplay.imageSliceView = new ImageSliceView( imageDisplay, bdvHandle, ( List< String > name ) -> moBIE2.openSourceAndConverters( name ) );
	}

	// TODO: own class: SegmentationDisplayConfigurator
	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		fetchSegmentsFromTables( segmentationDisplay );

		segmentationDisplay.segmentAdapter = new SegmentAdapter( segmentationDisplay.segments );
		ColoringModelHelper.configureMoBIEColoringModel( segmentationDisplay );
		segmentationDisplay.selectionModel = new DefaultSelectionModel<>();
		segmentationDisplay.coloringModel.setSelectionModel(  segmentationDisplay.selectionModel );

		// set selected segments
		if ( segmentationDisplay.getSelectedSegmentIds() != null )
		{
			final List< TableRowImageSegment > segments = segmentationDisplay.segmentAdapter.getSegments( segmentationDisplay.getSelectedSegmentIds() );
			segmentationDisplay.selectionModel.setSelected( segments, true );
		}

		initSliceViewer( segmentationDisplay );
		initTableViewer( segmentationDisplay );
		initScatterPlotViewer( segmentationDisplay );
		initVolumeViewer( segmentationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			WindowArrangementHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
		} );
	}

	private void fetchSegmentsFromTables( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.segments = new ArrayList<>();

		// load default tables
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );

			segmentationDisplay.segments.addAll( createAnnotatedImageSegmentsFromTableFile(
					moBIE2.getDefaultTablePath( source ),
					sourceName ) );
		}

		// load additional tables
		// TODO: This will not work like this for the grid view with multiple sources...
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );

			final List< String > tables = segmentationDisplay.getTables();
			if ( tables != null )
			{
				for ( String table : tables )
				{
					final String tablePath = moBIE2.getTablePath( source.tableDataLocation, table );
					IJ.log( "Opening table:\n" + tablePath );
					final Map< String, List< String > > newColumns =
							TableColumns.openAndOrderNewColumns(
									segmentationDisplay.segments,
									Constants.SEGMENT_LABEL_ID,
									tablePath );
					newColumns.remove( Constants.SEGMENT_LABEL_ID );
					for ( String columnName : newColumns.keySet() )
					{
						try
						{
							Object[] values = TableColumns.asTypedArray( newColumns.get( columnName ) );
							TableRows.addColumn( segmentationDisplay.segments, columnName, values );
						} catch ( UnsupportedDataTypeException e )
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void initSliceViewer( SegmentationDisplay segmentationDisplay )
	{
		final SegmentationImageSliceView segmentationImageSliceView = new SegmentationImageSliceView<>( segmentationDisplay, bdvHandle, ( List< String > names ) -> moBIE2.openSourceAndConverters( names ) );
		segmentationDisplay.segmentationImageSliceView = segmentationImageSliceView;
	}

	private void initVolumeViewer( SegmentationDisplay display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeView<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, universeManager );
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d() );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );
	}

	public synchronized void removeSourceDisplay( Display display )
	{
		if ( display instanceof SegmentationDisplay )
		{
			final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) display;
			segmentationDisplay.segmentationImageSliceView.close();
			segmentationDisplay.tableViewer.close();
			segmentationDisplay.scatterPlotViewer.close();
			segmentationDisplay.segmentsVolumeViewer.close();
		}
		else if ( display instanceof ImageDisplay )
		{
			final ImageDisplay imageDisplay = ( ImageDisplay ) display;
			imageDisplay.imageSliceView.close();
		}
		else if ( display instanceof GridOverlayDisplay )
		{
			final GridOverlayDisplay gridOverlayDisplay = ( GridOverlayDisplay ) display;
			gridOverlayDisplay.close();
		}

		userInterface.removeDisplaySettingsPanel( display );
		displays.remove( display );
	}

	public Collection< SegmentationDisplay > getSegmentationDisplays()
	{
		final List< SegmentationDisplay > segmentationDisplays = getSourceDisplays().stream().filter( s -> s instanceof SegmentationDisplay ).map( s -> ( SegmentationDisplay ) s ).collect( Collectors.toList() );

		return segmentationDisplays;
	}

	public void close()
	{
		removeAllSourceDisplays();
		sliceViewer.getBdvHandle().close();
		universeManager.close();
	}
}
