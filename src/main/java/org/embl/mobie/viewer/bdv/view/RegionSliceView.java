package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import net.imglib2.type.numeric.integer.IntType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.annotate.TableRowsIntervalImage;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.transform.PositionViewerTransform;


public class RegionSliceView extends AnnotationSliceView< RegionTableRow >
{
	public RegionSliceView( MoBIE moBIE, RegionDisplay display )
	{
		super( moBIE, display );
		SourceAndConverter< IntType > regionSourceAndConverter = createRegionSourceAndConverter();
		show( regionSourceAndConverter );
	}

	private SourceAndConverter< IntType > createRegionSourceAndConverter()
	{
		final TableRowsIntervalImage< RegionTableRow > intervalImage = new TableRowsIntervalImage<>( display.tableRows, display.coloringModel, display.getName() );

		return intervalImage.getSourceAndConverter();
	}

	@Override
	public synchronized void focusEvent( RegionTableRow selection, Object initiator )
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

		SliceViewLocationChanger.changeLocation( bdvHandle, new PositionViewerTransform( center, state.getCurrentTimepoint() ) );
	}

	private double[] getPosition( RegionTableRow selection )
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
