package de.embl.cba.mobie2;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.color.SegmentsConverter;
import de.embl.cba.mobie2.view.ImageViewer;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class Viewer
{
	private final MoBIE moBIE;
	private final ImageViewer imageViewer;

	public Viewer( MoBIE moBIE )
	{
		this.moBIE = moBIE;
		this.imageViewer = new ImageViewer( moBIE );
	}

	public void show( View view )
	{
		// Apply all sourceTransforms
		// ...

		// Show the sources
		for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
		{
			final SourceDisplays sourceDisplays = displaySupplier.get();

			if ( sourceDisplays instanceof ImageDisplays )
			{
				showImageDisplay( ( ImageDisplays ) sourceDisplays );
			}
			else if ( sourceDisplays instanceof SegmentationDisplays )
			{

			}


		}
	}

	private void showImageDisplay( ImageDisplays sourceDisplays )
	{
		final List< SourceAndConverter< ? > > sourceAndConverters = imageViewer.show( sourceDisplays );
	}

	private void showSegmentationDisplay( SegmentationDisplays segmentationDisplays )
	{
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > lazyCategoryColoringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		ColoringModelWrapper< TableRowImageSegment > coloringModel = new ColoringModelWrapper<>( lazyCategoryColoringModel, selectionModel );

		// TODO: make a list of the segments from all sources (loop)
		String sourceName = segmentationDisplays.sources.get( 0 );
		final SegmentationSource source = ( SegmentationSource ) moBIE.getSource( sourceName );
		List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile(
				moBIE.getAbsoluteDefaultTableLocation( source ),
				sourceName );
		final HashMap< LabelFrameAndImage, TableRowImageSegment > labelMap = SegmentUtils.createSegmentMap( segments );

		imageViewer.show( segmentationDisplays, coloringModel, labelMap );

	}
}
