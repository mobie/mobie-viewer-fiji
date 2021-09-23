package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import org.embl.mobie.io.util.source.LabelSource;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
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
import java.util.HashMap;
import java.util.Map;

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

		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : display.getSources() ) {
			sourceNameToSourceAndConverter.put( name, moBIE.getSourceAndConverter( name ) );
		}

		// convert to labelSource
		sourceNameToSourceAndConverter = asLabelSources( sourceNameToSourceAndConverter );

		display.sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : sourceNameToSourceAndConverter.keySet() )
		{
			SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( name );
			// set opacity
			OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

			// show
			displayService.show( bdvHandle, display.isVisible(), sourceAndConverter );

			// set blending mode
			if ( display.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );

			bdvHandle.getViewerPanel().addTimePointListener( ( TimePointListener ) sourceAndConverter.getConverter() );

			display.sourceNameToSourceAndConverter.put( name, sourceAndConverter );
		}
	}

	private Map< String, SourceAndConverter< ? > > asLabelSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		Map< String, SourceAndConverter< ? > > sourceNameToLabelSourceAndConverter = new HashMap<>();

		for ( String name: sourceNameToSourceAndConverter.keySet() )
		{
			SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( name );
			LabelConverter< S > labelConverter = new LabelConverter(
					display.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					display.coloringModel );

			SourceAndConverter< ? > sourceAndLabelConverter = asSourceAndLabelConverter( sourceAndConverter, labelConverter );

			sourceNameToLabelSourceAndConverter.put( name, sourceAndLabelConverter );
		}

		return sourceNameToLabelSourceAndConverter;
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
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter );
		}
		display.sourceNameToSourceAndConverter.clear();
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
		final SourceAndConverter< ? > sourceAndConverter = display.sourceNameToSourceAndConverter.get( sourceName );
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
