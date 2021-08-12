package de.embl.cba.mobie.bdv.view;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.bdv.render.BlendingMode;
import de.embl.cba.mobie.color.OpacityAdjuster;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie.color.LabelConverter;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
import de.embl.cba.mobie.transform.SourceTransformer;
import de.embl.cba.mobie.transform.TransformHelper;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SegmentationSliceView< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final MoBIE moBIE;
	private final SegmentationSourceDisplay display;
	private BdvHandle bdvHandle;

	public SegmentationSliceView( MoBIE moBIE, SegmentationSourceDisplay display, BdvHandle bdvHandle )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.bdvHandle = bdvHandle;

		displayService = SourceAndConverterServices.getBdvDisplayService();
		show();
	}

	private void show( )
	{
		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		List< SourceAndConverter< ? > > sourceAndConverters = display.getSources().stream().map( name -> moBIE.getSourceAndConverter( name ) ).collect( Collectors.toList() );

		// convert to labelSource
		sourceAndConverters = asLabelSources( sourceAndConverters );

		display.sourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			// set opacity
			OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

			// show
			displayService.show( bdvHandle, display.isVisible(), sourceAndConverter );

			// set blending mode
			if ( display.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );

			bdvHandle.getViewerPanel().addTimePointListener( ( TimePointListener ) sourceAndConverter.getConverter() );

			display.sourceAndConverters.add( sourceAndConverter );
		}
	}

	private List< SourceAndConverter< ? > > asLabelSources( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		List< SourceAndConverter< ? > > labelSourceAndConverters = new ArrayList<>( );
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			LabelConverter< S > labelConverter = new LabelConverter(
					display.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					display.coloringModel );

			SourceAndConverter< ? > sourceAndLabelConverter = asSourceAndLabelConverter( sourceAndConverter, labelConverter );

			labelSourceAndConverters.add( sourceAndLabelConverter );
		}

		return labelSourceAndConverters;
	}

	private SourceAndConverter asSourceAndLabelConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	public void close()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter );
		}
		display.sourceAndConverters.clear();
	};

	@Override
	public synchronized void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void selectionChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void focusEvent( S selection )
	{
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
		final SourceAndConverter< ? > sourceAndConverter = Utils.getSourceAndConverter( display.sourceAndConverters, sourceName );
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

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

}
