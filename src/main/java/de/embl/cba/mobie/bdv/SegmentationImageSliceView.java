package de.embl.cba.mobie.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie.color.LabelConverter;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.open.SourceAndConverterSupplier;
import de.embl.cba.mobie.transform.SourceTransformer;
import de.embl.cba.mobie.transform.TransformerHelper;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SegmentationImageSliceView< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final SegmentationSourceDisplay segmentationDisplay;
	private BdvHandle bdvHandle;
	private final SourceAndConverterSupplier sourceAndConverterSupplier;

	public SegmentationImageSliceView( SegmentationSourceDisplay segmentationDisplay, BdvHandle bdvHandle, SourceAndConverterSupplier sourceAndConverterSupplier  )
	{
		this.segmentationDisplay = segmentationDisplay;
		this.bdvHandle = bdvHandle;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;

		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		show();
	}

	private void show( )
	{
		segmentationDisplay.selectionModel.listeners().add( this );
		segmentationDisplay.coloringModel.listeners().add( this );

		// open
		List< SourceAndConverter< ? > > sourceAndConverters = sourceAndConverterSupplier.get( segmentationDisplay.getSources() );

		// transform
		sourceAndConverters = TransformerHelper.transformSourceAndConverters( sourceAndConverters, segmentationDisplay.sourceTransformers );

		// convert to labelSource
		sourceAndConverters = asLabelSources( sourceAndConverters );

		// adjust opacity and show in BDV
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			( ( LabelConverter ) sourceAndConverter.getConverter() ).setOpacity( segmentationDisplay.getOpacity() );
			displayService.show( bdvHandle, sourceAndConverter );
			bdvHandle.getViewerPanel().addTimePointListener( (LabelConverter) sourceAndConverter.getConverter() );
		}

		segmentationDisplay.sourceAndConverters = sourceAndConverters;
	}

	private List< SourceAndConverter< ? > > asLabelSources( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		List< SourceAndConverter< ? > > labelSourceAndConverters = new ArrayList<>( );
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			LabelConverter< S > labelConverter = new LabelConverter(
					segmentationDisplay.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					segmentationDisplay.coloringModel );

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
		for ( SourceAndConverter< ? > sourceAndConverter : segmentationDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}
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
		if ( segmentationDisplay.sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : segmentationDisplay.sourceTransformers )
			{
				final AffineTransform3D transform = sourceTransformer.getTransform( sourceName );
				transform.apply( position, position );
			}
		}
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

	public void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}
	}
}
