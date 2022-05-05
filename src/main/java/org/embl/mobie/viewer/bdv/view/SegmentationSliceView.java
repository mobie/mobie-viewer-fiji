package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.MergedGridSource;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;

public class SegmentationSliceView extends AnnotatedRegionSliceView< TableRowImageSegment >
{
	public SegmentationSliceView( MoBIE moBIE, SegmentationSourceDisplay display )
	{
		super( moBIE, display );

		for ( String name : display.getSources() )
		{
			final SourceAndConverter< ? > sourceAndConverter = moBIE.getTransformedSourceAndConverter( name );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( display, sourceAndConverter );

			show( labelSourceAndConverter );
		}
	}

	private SourceAndConverter< ? > asLabelSourceAndConverter( SegmentationSourceDisplay display, SourceAndConverter< ? > sourceAndConverter )
	{
		LabelConverter labelConverter = getLabelConverter( display, sourceAndConverter );

		SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

		return labelSourceAndConverter;
	}

	private LabelConverter getLabelConverter( SegmentationSourceDisplay display, SourceAndConverter< ? > sourceAndConverter )
	{
		if ( MergedGridSource.instanceOf( sourceAndConverter ) )
		{
			// The source name is not the one from which the
			// image segments should be fetched.
			// Thus, the constructor where the source name
			// is determined from encoding in the label is chosen.
			return new LabelConverter(
					display.segmentAdapter,
					display.coloringModel );
		}
		else
		{
			return new LabelConverter(
					display.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					display.coloringModel );
		}
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	@Override
	public synchronized void focusEvent( TableRowImageSegment selection, Object initiator )
	{
		if ( initiator instanceof SliceViewRegionSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();

		if ( selection.timePoint() != state.getCurrentTimepoint() )
		{
			state.setCurrentTimepoint( selection.timePoint() );
		}

		final double[] position = new double[ 3 ];
		selection.localize( position );

		adaptPosition( position, selection.imageId() );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				500 ).run();
	}

	private void adaptPosition( double[] position, String sourceName )
	{
		// get source transform
		final SourceAndConverter< ? > sourceAndConverter = moBIE.getTransformedSourceAndConverter( sourceName );
		AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );

		// remove scaling, because the positions are in scaled units
		final VoxelDimensions voxelDimensions = sourceAndConverter.getSpimSource().getVoxelDimensions();
		final AffineTransform3D scalingTransform = new AffineTransform3D();
		scalingTransform.scale( voxelDimensions.dimension( 0 ), voxelDimensions.dimension( 1 ), voxelDimensions.dimension( 2 )  );
		sourceTransform = sourceTransform.concatenate( scalingTransform.inverse() );

		// adapt
		sourceTransform.apply( position, position );
	}
}
