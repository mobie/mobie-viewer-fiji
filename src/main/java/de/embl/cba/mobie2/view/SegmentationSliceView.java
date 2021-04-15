package de.embl.cba.mobie2.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.bdv.BdvLocationLogger;
import de.embl.cba.mobie2.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.mobie2.color.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.LabelConverter;
import de.embl.cba.mobie2.color.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.open.SourceAndConverterSupplier;
import de.embl.cba.mobie2.segment.BdvSegmentSelector;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import mpicbg.spim.data.SpimData;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.fife.rsta.ac.js.Logger;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.MinimalBdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourceAndConverterBlendingModeChangerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SegmentationSliceView< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final SegmentationDisplay segmentationDisplay;
	private BdvHandle bdvHandle;
	private final SourceAndConverterSupplier sourceAndConverterSupplier;

	public SegmentationSliceView( SegmentationDisplay segmentationDisplay, BdvHandle bdvHandle, SourceAndConverterSupplier sourceAndConverterSupplier  )
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

		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		// open
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			sourceAndConverters.add( sourceAndConverterSupplier.get( sourceName ) );
		}

		// transform
		List< SourceAndConverter< ? > > transformedSourceAndConverters = transformSourceAndConverters( sourceAndConverters, segmentationDisplay.sourceTransformers );

		// convert to labelSource
		for ( SourceAndConverter< ? > sourceAndConverter : transformedSourceAndConverters )
		{
			LabelConverter< S > labelConverter = new LabelConverter(
					segmentationDisplay.segmentAdapter,
					sourceAndConverter.getSpimSource().getName(),
					segmentationDisplay.coloringModel );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

			sourceAndConverters.remove( sourceAndConverter );
			sourceAndConverters.add( labelSourceAndConverter );

			displayService.show( bdvHandle, labelSourceAndConverter );
		}

		segmentationDisplay.sourceAndConverters = sourceAndConverters;
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	private List< SourceAndConverter< ? > > transformSourceAndConverters( List< SourceAndConverter< ? > > sourceAndConverters, List< SourceTransformer > sourceTransformers )
	{
		List< SourceAndConverter< ? > > transformed = new ArrayList<>( sourceAndConverters );
		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				transformed = sourceTransformer.transform( transformed );
			}
		}

		return transformed;
	}

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

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				500 ).run();
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
