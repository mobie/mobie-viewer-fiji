package de.embl.cba.mobie2.view;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.segment.SegmentAdapter;
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
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;


import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class SourceDisplayManager< T extends TableRow, S extends ImageSegment >
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final ImageViewer imageViewer;
	private ArrayList< SourceDisplay > sourceDisplays;

	public SourceDisplayManager( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
		imageViewer = new ImageViewer( moBIE2, is2D, this, timepoints );
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), imageViewer.getWindow(), false, true );
	}

	public ArrayList< SourceDisplay > getSourceDisplays()
	{
		return sourceDisplays;
	}

	public ImageViewer getImageViewer()
	{
		return imageViewer;
	}

	public void show( View view )
	{
		// Show the sources
		if ( view.sourceDisplays != null )
		{
			for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
			{
				showSourceDisplay( view, displaySupplier.get() );
			}
		}

		// Adjust the viewer transform
		// TODO
	}

	private void showSourceDisplay( View view, SourceDisplay sourceDisplay )
	{
		if ( sourceDisplays.contains( sourceDisplay ) ) return;

		if ( sourceDisplay.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		if ( sourceDisplay instanceof ImageDisplay )
		{
			showImageDisplay( ( ImageDisplay ) sourceDisplay, view.sourceTransforms );
		}
		else if ( sourceDisplay instanceof SegmentationDisplay )
		{
			showSegmentationDisplay( ( SegmentationDisplay ) sourceDisplay );
		}

		userInterface.addSourceDisplay( sourceDisplay );
		sourceDisplays.add( sourceDisplay );
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
		imageViewer.show( imageDisplay, sourceTransforms );

		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), imageDisplay.sourceAndConverters.get( 0 ) ).run();
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

		imageViewer.show( display );
		ViewerHelper.showInTableViewer( display );
		ViewerHelper.showInScatterPlotViewer( display );

		SwingUtilities.invokeLater( () ->
		{
			UserInterfaceHelper.bottomAlignWindow( display.imageViewer.getWindow(), display.tableViewer.getWindow() );
			UserInterfaceHelper.rightAlignWindow( display.imageViewer.getWindow(), display.scatterPlotViewer.getWindow(), true, true );
		} );
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		sourceDisplay.imageViewer.removeSourceDisplay( sourceDisplay );

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
