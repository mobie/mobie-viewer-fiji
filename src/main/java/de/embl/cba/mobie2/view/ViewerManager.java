package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.grid.GridView;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
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
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setLafSwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setSystemSwingLookAndFeel;

public class ViewerManager
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private ArrayList< Display > displays;
	private Image3DUniverse universe;
	private final BdvHandle bdvHandle;
	private GridView gridView;

	public ViewerManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		displays = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints );
		bdvHandle = sliceViewer.get();
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), sliceViewer.getWindow(), false, true );
	}

	public static void showInScatterPlotViewer( SegmentationDisplay display )
	{
		display.scatterPlotViewer = new ScatterPlotViewer<>( display.segments, display.selectionModel, display.coloringModel, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, new double[]{1.0, 1.0}, 0.5 );
		display.scatterPlotViewer.show();
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );
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

		setLafSwingLookAndFeel();

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

		setSystemSwingLookAndFeel();

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
						gridView = new GridView( moBIE2, bdvHandle,  "grid-view-" + (i++), tableDataLocation, ( GridSourceTransformer ) sourceTransformer );

						userInterface.addGridView( gridView );

						SwingUtilities.invokeLater( () ->
						{
							UserInterfaceHelper.bottomAlignWindow( window, gridView.getTableViewer().getWindow() );
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
		showInScatterPlotViewer( segmentationDisplay );
		initSegmentsVolumeViewer( segmentationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			UserInterfaceHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
			UserInterfaceHelper.rightAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.scatterPlotViewer.getWindow(), true, true );
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
			( ( SegmentationDisplay ) display ).segmentationImageSliceView.close();
			( ( SegmentationDisplay ) display ).tableViewer.getWindow().dispose();
			( ( SegmentationDisplay ) display ).scatterPlotViewer.getWindow().dispose();
		}
		else
		{
			( ( ImageDisplay ) display ).imageSliceView.close();
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
