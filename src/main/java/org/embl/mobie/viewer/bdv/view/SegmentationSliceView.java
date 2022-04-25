package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.MergedGridSource;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class SegmentationSliceView extends AnnotatedRegionSliceView< TableRowImageSegment >
{

	public SegmentationSliceView( MoBIE moBIE, SegmentationSourceDisplay display, BdvHandle bdvHandle )
	{
		super( moBIE, display, bdvHandle );

		for ( String name : display.getSources() )
		{
			final SourceAndConverter< ? > sourceAndConverter = moBIE.getTransformedSourceAndConverter( name );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( display, sourceAndConverter );

			show( labelSourceAndConverter );

			if ( display.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( labelSourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );
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
		LabelConverter labelConverter;

		if ( MergedGridSource.instanceOf( sourceAndConverter ) )
		{
			// The source name is not the one from which the
			// image segments should be fetched.
			// Thus, the constructor where the source name
			// is determined from encoding in the label is chosen.
			labelConverter = new LabelConverter(
					display.segmentAdapter,
					display.coloringModel );
		}
		else
		{
			labelConverter = new LabelConverter(
					display.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					display.coloringModel );
		}
		return labelConverter;
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	@Override
	public synchronized void focusEvent( TableRowImageSegment selection, Object origin  )
	{
		if ( origin instanceof SliceViewRegionSelector )
			return;

		if ( selection.timePoint() != getBdvHandle().getViewerPanel().state().getCurrentTimepoint() )
		{
			getBdvHandle().getViewerPanel().state().setCurrentTimepoint( selection.timePoint() );
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
