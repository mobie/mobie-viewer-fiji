package de.embl.cba.mobie2.view;

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
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
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
	private final BdvViewer bdvViewer;
	private ArrayList< SourceDisplay > sourceDisplays;
	private Image3DUniverse universe;

	public ViewerManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
		bdvViewer = new BdvViewer( moBIE2, is2D, this, timepoints );
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), bdvViewer.getWindow(), false, true );
	}

	public static void showInScatterPlotViewer( SegmentationDisplay display )
	{
		display.scatterPlotViewer = new ScatterPlotViewer<>( display.segments, display.selectionModel, display.coloringModel, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, new double[]{1.0, 1.0}, 0.5 );
		display.scatterPlotViewer.show();
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.bdvViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );
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

	public BdvViewer getImageViewer()
	{
		return bdvViewer;
	}

	public void show( View view )
	{
		// Show the sources
		setLafSwingLookAndFeel();
		if ( view.sourceDisplays != null )
		{
			for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
			{
				showSourceDisplay( displaySupplier.get(), view.sourceTransforms );
			}
		}
		setSystemSwingLookAndFeel();

		// Adjust the viewer transform
		// TODO


	}

	private void showSourceDisplay( SourceDisplay sourceDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		if ( sourceDisplays.contains( sourceDisplay ) ) return;

		if ( sourceDisplay.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		if ( sourceDisplay instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) sourceDisplay, sourceTransforms );
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
			universe.show();
			// Bug on MAC causes crash if users try to resize
			//universe.getWindow().setResizable( false );
		}

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

	private void showImageDisplay( ImageDisplay imageDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		bdvViewer.show( imageDisplay, sourceTransforms );

		new ViewerTransformAdjuster( bdvViewer.getBdvHandle(), imageDisplay.sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay display )
	{
		display.coloringModel = new MoBIEColoringModel<>( display.getLut() );
		display.selectionModel = new DefaultSelectionModel<>();
		display.coloringModel.setSelectionModel(  display.selectionModel );

		if ( display.getSources().size() > 1 )
		{
			throw new UnsupportedOperationException( "Multiple segmentation sources are not yet implemented." );
			// TODO: make a list of the segments from all sources (loop)
		}

		String sourceName = display.getSources().get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
		display.segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE2.getDefaultTableLocation( source ),
				sourceName );

		display.segmentAdapter = new SegmentAdapter( display.segments );
		if ( display.getSelectedSegmentIds() != null )
		{
			// TODO: add to selection model
			display.segmentAdapter.getSegments( display.getSelectedSegmentIds() );
		}

		bdvViewer.show( display );
		showInTableViewer( display );
		showInScatterPlotViewer( display );
		initSegmentsVolumeViewer( display );

		SwingUtilities.invokeLater( () ->
		{
			UserInterfaceHelper.bottomAlignWindow( display.bdvViewer.getWindow(), display.tableViewer.getWindow() );
			UserInterfaceHelper.rightAlignWindow( display.bdvViewer.getWindow(), display.scatterPlotViewer.getWindow(), true, true );
		} );
	}

	private void initSegmentsVolumeViewer( SegmentationDisplay display )
	{
		display.segmentsVolumeViewer = new Segments3DViewer<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, ()  -> getUniverse()  );
		display.segmentsVolumeViewer.setShowSegments( display.showSelectedSegmentsIn3d() );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		sourceDisplay.bdvViewer.removeSourceDisplay( sourceDisplay );

		if ( sourceDisplay instanceof SegmentationDisplay )
		{
			( ( SegmentationDisplay ) sourceDisplay ).tableViewer.getWindow().dispose();
			( ( SegmentationDisplay ) sourceDisplay ).scatterPlotViewer.getWindow().dispose();

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
