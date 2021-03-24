package de.embl.cba.mobie2.view;

import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.select.SelectionModelAndLabelAdapter;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

import java.util.HashMap;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class Viewer
{
	private final MoBIE2 moBIE2;
	private final UserInterface userInterface;
	private final ImageViewer imageViewer;
	private UserInterfaceHelper userInterfaceHelper;

	public Viewer( MoBIE2 moBIE2, UserInterface userInterface, boolean is2D )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		imageViewer = new ImageViewer( moBIE2, is2D );
	}

	public ImageViewer getImageViewer()
	{
		return imageViewer;
	}

	public void show( View view )
	{
		// Apply all sourceTransforms
		// ...

		// Show the sources
		for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
		{
			final SourceDisplay sourceDisplay = displaySupplier.get();

			if ( sourceDisplay instanceof ImageDisplay )
			{
				final ImageDisplay imageDisplay = ( ImageDisplay ) sourceDisplay;
				showImageDisplay( imageDisplay );
			}
			else if ( sourceDisplay instanceof SegmentationDisplay )
			{
				final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) sourceDisplay;
				showSegmentationDisplay( segmentationDisplay );
			}

			userInterface.addDisplaySettings( sourceDisplay );
		}
	}

	private void showImageDisplay( ImageDisplay display )
	{
		display.sourceAndConverters = imageViewer.show( display );

		new BrightnessAutoAdjuster( display.sourceAndConverters.get( 0 ),0  ).run();
		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), display.sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay display )
	{
		display.selectionModel = new DefaultSelectionModel< TableRowImageSegment >();
		display.coloringModel = new ColoringModelWrapper<>( display.selectionModel );

		if ( display.sources.size() > 1 )
		{
			throw new UnsupportedOperationException( "Multiple segmentation sources are not yet implemented." );
			// TODO: make a list of the segments from all sources (loop)
		}

		String sourceName = display.sources.get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
		List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE2.getAbsoluteDefaultTableLocation( source ),
				sourceName );
		final HashMap< LabelFrameAndImage, TableRowImageSegment > labelToSegmentAdapter = SegmentUtils.createSegmentMap( segments );

		// show in imageViewer
		//
		final SelectionModelAndLabelAdapter selectionModelAndAdapter = new SelectionModelAndLabelAdapter( display.selectionModel, labelToSegmentAdapter );
		display.sourceAndConverters = imageViewer.show( display, display.coloringModel, selectionModelAndAdapter );
		display.selectionModel.listeners().add( imageViewer );
		display.coloringModel.listeners().add( imageViewer );

		// show in tableViewer
		//
		display.tableViewer = new TableViewer<>( segments, display.selectionModel, display.coloringModel, display.name );
		display.tableViewer.show( imageViewer.getBdvHandle().getViewerPanel() );

		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );


	}
}
