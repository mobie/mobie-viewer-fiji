package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.serialize.View;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij3d.Image3DUniverse;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;


import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setLafSwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setSystemSwingLookAndFeel;

public class ViewerManager< T extends TableRow, S extends ImageSegment >
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private ArrayList< SourceDisplay > sourceDisplays;
	private Image3DUniverse universe;
	private final BdvHandle bdvHandle;

	public ViewerManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
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

	public ArrayList< SourceDisplay > getSourceDisplays()
	{
		return sourceDisplays;
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
		// Show the sources
		setLafSwingLookAndFeel();
		if ( view.sourceDisplays != null )
		{
			for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
			{
				final SourceDisplay sourceDisplay = displaySupplier.get();

				if ( sourceDisplay.sourceTransformers != null )
					sourceDisplay.sourceTransformers = view.sourceTransforms.stream().map( s -> s.get() ).collect( Collectors.toList() );

				showSourceDisplay( sourceDisplay );
			}
		}
		setSystemSwingLookAndFeel();

		// Adjust the viewer transform
		// TODO
	}

	private void showSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplays.contains( sourceDisplay ) ) return;

		if ( sourceDisplay.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		sourceDisplay.sliceViewer = sliceViewer;

		if ( sourceDisplay instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationDisplay )
		{
			final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) sourceDisplay;
			showSegmentationDisplay( segmentationDisplay );
		}

		userInterface.addSourceDisplay( sourceDisplay );
		sourceDisplays.add( sourceDisplay );
	}

	private Image3DUniverse getUniverse()
	{
		if ( universe == null )
		{
			universe = new Image3DUniverse();
			// Bug on MAC causes crash if users try to resize
			//universe.getWindow().setResizable( false );
		}

		universe.show();

		return universe;
	}

	private void removeAllSourceDisplays()
	{
		// create a copy of the currently shown displays...
		final ArrayList< SourceDisplay > currentDisplays = new ArrayList<>( sourceDisplays ) ;

		// ...such that we can remove them without
		// modifying the list that we iterate over
		for ( SourceDisplay display : currentDisplays )
		{
			// removes from all viewers and
			// also from sourceDisplays
			removeSourceDisplay( display );
		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay )
	{
		final ImageSliceView imageSliceView = new ImageSliceView( imageDisplay, bdvHandle, ( String name ) -> moBIE2.getSourceAndConverter( name ) );
		imageSliceView.show();
		imageDisplay.imageSliceView = imageSliceView;

		new ViewerTransformAdjuster( sliceViewer.getBdvHandle(), imageDisplay.sourceAndConverters.get( 0 ) ).run();
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
		final SegmentationSliceView segmentationSliceView = new SegmentationSliceView<>( segmentationDisplay, bdvHandle, ( String name ) -> moBIE2.getSourceAndConverter( name ) );
		segmentationDisplay.segmentationSliceView = segmentationSliceView;
	}

	private void initSegmentsVolumeViewer( SegmentationDisplay display )
	{
		display.segmentsVolumeViewer = new Segments3DView<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, () -> getUniverse()  );
		display.segmentsVolumeViewer.setShowSegments( display.showSelectedSegmentsIn3d() );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof SegmentationDisplay )
		{
			( ( SegmentationDisplay ) sourceDisplay ).segmentationSliceView.close();
			( ( SegmentationDisplay ) sourceDisplay ).tableViewer.getWindow().dispose();
			( ( SegmentationDisplay ) sourceDisplay ).scatterPlotViewer.getWindow().dispose();
		}
		else
		{
			( ( ImageDisplay ) sourceDisplay ).imageSliceView.close();
		}

		userInterface.removeSourceDisplay( sourceDisplay );
		sourceDisplays.remove( sourceDisplay );
	}

	public Collection< SegmentationDisplay > getSegmentationDisplays()
	{
		final List< SegmentationDisplay > segmentationDisplays = getSourceDisplays().stream().filter( s -> s instanceof SegmentationDisplay ).map( s -> ( SegmentationDisplay ) s ).collect( Collectors.toList() );

		return segmentationDisplays;
	}
}
