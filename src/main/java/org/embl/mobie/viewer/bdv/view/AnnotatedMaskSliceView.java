package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotate.AnnotatedMaskTableRow;
import org.embl.mobie.viewer.annotate.TableRowsIntervalImage;
import org.embl.mobie.viewer.color.ListItemsARGBConverter;
import org.embl.mobie.viewer.display.AnnotatedSourceDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import org.embl.mobie.viewer.transform.PositionViewerTransform;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformChanger;
import net.imglib2.type.numeric.integer.IntType;


public class AnnotatedMaskSliceView extends AnnotatedRegionSliceView< AnnotatedMaskTableRow >
{
	public AnnotatedMaskSliceView( MoBIE moBIE, AnnotatedSourceDisplay display, BdvHandle bdvHandle  )
	{
		super( moBIE, display, bdvHandle );

		final SourceAndConverter< IntType > sourceAndConverter = createSourceAndConverter();

		show( sourceAndConverter );
	}

	private SourceAndConverter< IntType > createSourceAndConverter()
	{
		final TableRowsIntervalImage< AnnotatedMaskTableRow > maskImage = new TableRowsIntervalImage<>( display.tableRows, display.coloringModel, display.getName() );

		final SourceAndConverter< IntType > sourceAndConverter = maskImage.getSourceAndConverter();
		return sourceAndConverter;
	}

	@Override
	public synchronized void focusEvent( AnnotatedMaskTableRow selection, Object origin  )
	{
		if ( origin instanceof SliceViewRegionSelector )
			return;

		if ( selection.timePoint() != getBdvHandle().getViewerPanel().state().getCurrentTimepoint() )
		{
			getBdvHandle().getViewerPanel().state().setCurrentTimepoint( selection.timePoint() );
		}

		final double[] center = getPosition( selection );

		MoBIEViewerTransformChanger.changeViewerTransform( bdvHandle, new PositionViewerTransform( center, bdvHandle.getViewerPanel().state().getCurrentTimepoint() ) );
	}

	private double[] getPosition( AnnotatedMaskTableRow selection )
	{
		final double[] max = selection.mask().maxAsDoubleArray();
		final double[] min = selection.mask().minAsDoubleArray();
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < 3; d++ )
		{
			center[ d ] = ( max[ d ] + min[ d ] ) / 2;
		}
		return center;
	}
}
