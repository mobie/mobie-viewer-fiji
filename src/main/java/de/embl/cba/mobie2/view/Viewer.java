package de.embl.cba.mobie2.view;

import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

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
		// Show the sources
		if ( view.sourceDisplays != null )
		{
			for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
			{
				final SourceDisplay sourceDisplay = displaySupplier.get();

				if ( sourceDisplay instanceof ImageDisplay )
				{
					showImageDisplay( ( ImageDisplay ) sourceDisplay, view.sourceTransforms );
				} else if ( sourceDisplay instanceof SegmentationDisplay )
				{
					showSegmentationDisplay( ( SegmentationDisplay ) sourceDisplay );
				}

				userInterface.addDisplaySettings( sourceDisplay );
			}
		}

		// Adjust the viewer transform
	}

	private void showImageDisplay( ImageDisplay sourceDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		sourceDisplay.imageViewer = imageViewer;
		sourceDisplay.sourceAndConverters = imageViewer.show( sourceDisplay, sourceTransforms );

		new BrightnessAutoAdjuster( sourceDisplay.sourceAndConverters.get( 0 ),0  ).run();
		new ViewerTransformAdjuster( imageViewer.getBdvHandle(), sourceDisplay.sourceAndConverters.get( 0 ) ).run();
	}

	private void showSegmentationDisplay( SegmentationDisplay display )
	{
		display.imageViewer = imageViewer;

		display.selectionModel = new DefaultSelectionModel< TableRowImageSegment >();
		display.coloringModel = new ColoringModelWrapper<>( display.selectionModel );

		if ( display.sources.size() > 1 )
		{
			throw new UnsupportedOperationException( "Multiple segmentation sources are not yet implemented." );
			// TODO: make a list of the segments from all sources (loop)
		}

		String sourceName = display.sources.get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
		display.segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE2.getAbsoluteDefaultTableLocation( source ),
				sourceName );

		ViewerHelper.showInImageViewer( display );
		ViewerHelper.showInTableViewer( display );
		ViewerHelper.showInScatterPlotViewer( display );
	}

}
