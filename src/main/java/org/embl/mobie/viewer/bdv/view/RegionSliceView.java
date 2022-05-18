package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import net.imglib2.type.numeric.integer.IntType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotate.AnnotatedMaskTableRow;
import org.embl.mobie.viewer.annotate.TableRowsIntervalImage;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformChanger;
import org.embl.mobie.viewer.transform.PositionViewerTransform;


public class RegionSliceView extends AnnotationSliceView< AnnotatedMaskTableRow >
{
	public RegionSliceView( MoBIE moBIE, RegionDisplay display )
	{
		super( moBIE, display );

		final SourceAndConverter< IntType > sourceAndConverter = createSourceAndConverter();

		display.sliceViewer.show( sourceAndConverter, display );
	}

	private SourceAndConverter< IntType > createSourceAndConverter()
	{
		final TableRowsIntervalImage< AnnotatedMaskTableRow > maskImage = new TableRowsIntervalImage<>( display.tableRows, display.coloringModel, display.getName() );

		return maskImage.getSourceAndConverter();
	}

	@Override
	public synchronized void focusEvent( AnnotatedMaskTableRow selection, Object initiator )
	{
		if ( initiator instanceof SliceViewRegionSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();

		if ( selection.timePoint() != state.getCurrentTimepoint() )
		{
			state.setCurrentTimepoint( selection.timePoint() );
		}

		final double[] center = getPosition( selection );

		MoBIEViewerTransformChanger.changeViewerTransform( bdvHandle, new PositionViewerTransform( center, state.getCurrentTimepoint() ) );
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
