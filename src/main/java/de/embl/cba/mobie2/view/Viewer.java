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
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class Viewer< T extends TableRow, S extends ImageSegment >
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final ImageViewer imageViewer;
	private UserInterfaceHelper userInterfaceHelper;
	private ArrayList< SourceDisplay > sourceDisplays;

	public Viewer( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
		imageViewer = new ImageViewer( moBIE2, is2D );
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), imageViewer.getWindow(), false, true );
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
	}

	private void showSourceDisplay( View view, SourceDisplay sourceDisplay )
	{
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
		for ( SourceDisplay display : sourceDisplays )
		{
			remove( display );
		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		imageDisplay.imageViewer = imageViewer;
		imageDisplay.sourceAndConverters = imageViewer.show( imageDisplay, sourceTransforms );

		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), imageDisplay.sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay display )
	{
		display.imageViewer = imageViewer;

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
				moBIE2.getAbsoluteDefaultTableLocation( source ),
				sourceName );

		display.segmentAdapter = new SegmentAdapter( display.segments );
		display.segmentAdapter.getSegments( display.getSelectedSegmentIds() );

		ViewerHelper.showInImageViewer( display );
		ViewerHelper.showInTableViewer( display );
		ViewerHelper.showInScatterPlotViewer( display );

		SwingUtilities.invokeLater( () ->
		{
			UserInterfaceHelper.bottomAlignWindow( display.imageViewer.getWindow(), display.tableViewer.getWindow() );
			UserInterfaceHelper.rightAlignWindow( display.imageViewer.getWindow(), display.scatterPlotViewer.getWindow(), true, true );
		} );
	}

	public void remove( SourceDisplay sourceDisplay )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		if ( sourceDisplay instanceof SegmentationDisplay )
		{
			( ( SegmentationDisplay ) sourceDisplay ).tableViewer.getWindow().dispose();
			( ( SegmentationDisplay ) sourceDisplay ).scatterPlotViewer.getWindow().dispose();
		}

		userInterface.removeSourceDisplay( sourceDisplay );
		sourceDisplays.remove( sourceDisplay );
	}
}
