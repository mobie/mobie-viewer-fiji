package de.embl.cba.mobie2;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.select.SelectionModelAndLabelAdapter;
import de.embl.cba.mobie2.view.ImageViewer;
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
	private final MoBIE moBIE;
	private final ImageViewer imageViewer;

	public Viewer( MoBIE moBIE, boolean is2D )
	{
		this.moBIE = moBIE;
		this.imageViewer = new ImageViewer( moBIE, is2D );
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
				showImageDisplay( ( ImageDisplay ) sourceDisplay );
			}
			else if ( sourceDisplay instanceof SegmentationDisplay )
			{
				showSegmentationDisplay( ( SegmentationDisplay ) sourceDisplay );
			}


		}
	}

	private void showImageDisplay( ImageDisplay sourceDisplays )
	{
		final List< SourceAndConverter< ? > > sourceAndConverters = imageViewer.show( sourceDisplays );

		new BrightnessAutoAdjuster( sourceAndConverters.get( 0 ),0  ).run();
		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		ColoringModelWrapper< TableRowImageSegment > coloringModel = new ColoringModelWrapper<>( new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) ), selectionModel );

		// TODO: make a list of the segments from all sources (loop)
		String sourceName = segmentationDisplay.sources.get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE.getSource( sourceName );
		List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE.getAbsoluteDefaultTableLocation( source ),
				sourceName );
		final HashMap< LabelFrameAndImage, TableRowImageSegment > labelMap = SegmentUtils.createSegmentMap( segments );

		// show in imageViewer

		final SelectionModelAndLabelAdapter selectionModelAndAdapter = new SelectionModelAndLabelAdapter( selectionModel, labelMap );
		final List< SourceAndConverter< ? > > sourceAndConverters = imageViewer.show( segmentationDisplay, coloringModel, selectionModelAndAdapter );
		coloringModel.listeners().add( imageViewer );
		selectionModel.listeners().add( imageViewer );

		// show in tableViewer
	}
}
