package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.bdv.ImageSliceView;
import de.embl.cba.mobie2.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie2.bdv.SliceViewer;
import de.embl.cba.mobie2.grid.GridOverlayDisplay;
import de.embl.cba.mobie2.plot.ScatterPlotViewer;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.table.TableViewer;
import de.embl.cba.mobie2.transform.GridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij3d.Image3DUniverse;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	private Image3DUniverse universe;
	private final BdvHandle bdvHandle;
	private GridOverlayDisplay gridOverlayDisplay;

	public ViewerManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		displays = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints );
		bdvHandle = sliceViewer.get();
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), sliceViewer.getWindow(), false, true );
	}

	public static void showScatterPlotViewer( SegmentationDisplay display )
	{
		display.scatterPlotViewer = new ScatterPlotViewer<>( display.segments, display.selectionModel, display.coloringModel, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );

		if ( display.showScatterPlot() )
			display.scatterPlotViewer.show();
	}

	public static void showInTableViewer( SegmentationDisplay display  )
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

	/**
	 *
	 *
	 * @param view
	 * 					Serialised view
	 */
	public void show( View view )
	{
		if ( view.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		setMoBIESwingLookAndFeel();

		final List< SourceDisplaySupplier > sourceDisplays = view.getSourceDisplays();

		List< SourceTransformer > sourceTransformers = null;
		if ( view.getSourceTransforms() != null )
			sourceTransformers = view.getSourceTransforms().stream().map( s -> s.get() ).collect( Collectors.toList() );

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

		// Adjust the viewer transform
		// TODO
		//
		new ViewerTransformAdjuster( bdvHandle, displays.get( displays.size() - 1 ).sourceAndConverters.get( 0 ) ).run();
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
							UserInterfaceHelper.bottomAlignWindow( window, gridOverlayDisplay.getTableViewer().getWindow() );
						} );
					}
				}
			}
		}
	}

	private synchronized Image3DUniverse getUniverse()
	{
		if ( universe == null )
		{
			universe = new Image3DUniverse();
			universe.show();
			// Bug on MAC causes crash if users try to resize
			//universe.getWindow().setResizable( false );
		}

		return universe;
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
		imageDisplay.imageSliceView = new ImageSliceView( imageDisplay, bdvHandle, ( String name ) -> moBIE2.getSourceAndConverter( name ) );
	}

	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.coloringModel = new MoBIEColoringModel<>( segmentationDisplay.getLut() );
		segmentationDisplay.selectionModel = new DefaultSelectionModel<>();
		segmentationDisplay.coloringModel.setSelectionModel(  segmentationDisplay.selectionModel );

		segmentationDisplay.segments = new ArrayList<>();
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
			final List< TableRowImageSegment > segmentsFromTableFile = createAnnotatedImageSegmentsFromTableFile(
					moBIE2.getDefaultTableLocation( source ),
					sourceName );

			segmentationDisplay.segments.addAll( segmentsFromTableFile );
		}

		segmentationDisplay.segmentAdapter = new SegmentAdapter( segmentationDisplay.segments );
		if ( segmentationDisplay.getSelectedSegmentIds() != null )
		{
			// TODO: add to selection model
			segmentationDisplay.segmentAdapter.getSegments( segmentationDisplay.getSelectedSegmentIds() );
		}

		showInSliceViewer( segmentationDisplay );
		showInTableViewer( segmentationDisplay );
		showScatterPlotViewer( segmentationDisplay );
		initSegmentsVolumeViewer( segmentationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			UserInterfaceHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
		} );
	}

	private void showInSliceViewer( SegmentationDisplay segmentationDisplay )
	{
		final SegmentationImageSliceView segmentationImageSliceView = new SegmentationImageSliceView<>( segmentationDisplay, bdvHandle, ( String name ) -> moBIE2.getSourceAndConverter( name ) );
		segmentationDisplay.segmentationImageSliceView = segmentationImageSliceView;
	}

	private void initSegmentsVolumeViewer( SegmentationDisplay display )
	{
		display.segmentsVolumeViewer = new Segments3DView<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, () -> getUniverse()  );
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
}
