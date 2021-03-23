package de.embl.cba.mobie2;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.select.SelectionModelAndLabelAdapter;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.mobie2.view.ImageViewer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
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

			userInterface.addDisplaySettingsPanel( sourceDisplay );

		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay )
	{
		imageDisplay.sourceAndConverters = imageViewer.show( imageDisplay );

		new BrightnessAutoAdjuster( imageDisplay.sourceAndConverters.get( 0 ),0  ).run();
		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), imageDisplay.sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.selectionModel = new DefaultSelectionModel< TableRowImageSegment >();
		segmentationDisplay.coloringModel = new MoBIEColoringModel<>( new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) ), segmentationDisplay.selectionModel );



		// TODO: make a list of the segments from all sources (loop)
		String sourceName = segmentationDisplay.sources.get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
		List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE2.getAbsoluteDefaultTableLocation( source ),
				sourceName );
		final HashMap< LabelFrameAndImage, TableRowImageSegment > labelToSegmentAdapter = SegmentUtils.createSegmentMap( segments );

		// show in imageViewer
		//
		final SelectionModelAndLabelAdapter selectionModelAndAdapter = new SelectionModelAndLabelAdapter( segmentationDisplay.selectionModel, labelToSegmentAdapter );
		segmentationDisplay.sourceAndConverters = imageViewer.show( segmentationDisplay, segmentationDisplay.coloringModel, selectionModelAndAdapter );
		segmentationDisplay.coloringModel.listeners().add( imageViewer );
		segmentationDisplay.coloringModel.listeners().add( imageViewer );

		// show in tableViewer
		//
		final TableViewer< TableRowImageSegment > tableViewer = new TableViewer<>( segments, selectionModel, coloringModel, segmentationDisplay.name );
		tableViewer.showTableAndMenu();
		coloringModel.listeners().add( tableViewer );
		selectionModel.listeners().add( tableViewer );
	}
}
