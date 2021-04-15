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
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class SliceViewer< S extends ImageSegment > implements Supplier< BdvHandle >
{
	private final MoBIE2 moBIE2;
	private final SourceAndConverterBdvDisplayService displayService;
	private BdvHandle bdvHandle;
	private final boolean is2D;
	private final ViewerManager< ?, ? > viewerManager;
	private final int timepoints;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;
	private List< SourceDisplay > sourceDisplays;

	public SliceViewer( MoBIE2 moBIE2, boolean is2D, ViewerManager viewerManager, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.is2D = is2D;
		this.viewerManager = viewerManager;
		this.timepoints = timepoints;

		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		// init other stuff
		sourceDisplays = new ArrayList<>();

		// register context menu actions
		installContextMenu();
	}

	@Override
	public BdvHandle get()
	{
		bdvHandle = createBdv( timepoints );
		displayService.registerBdvHandle( bdvHandle );
		return bdvHandle;
	}

	private void installContextMenu( )
	{
		final Set< String > actionsKeys = sacService.getActionsKeys();
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		final String[] actions = {
				sacService.getCommandName( ScreenShotMakerCommand.class ),
				sacService.getCommandName( BdvLocationLogger.class ),
				sacService.getCommandName( SourceAndConverterBlendingModeChangerCommand.class ) };

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );
		final BdvSegmentSelector segmentBdvSelector = new BdvSegmentSelector( bdvHandle, is2D, () -> viewerManager.getSegmentationDisplays() );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> segmentBdvSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;
	}

	private BdvHandle createBdv( int numTimepoints )
	{
		// create Bdv
		final MinimalBdvCreator bdvCreator = new MinimalBdvCreator( "MoBIE", is2D, Projector.MIXED_PROJECTOR, true, numTimepoints );
		final BdvHandle bdvHandle = bdvCreator.get();

		// configure size and location on screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() ).setSize( UserInterfaceHelper.getDefaultWindowWidth(), (int) ( screenSize.height * 0.7 ) );

		return bdvHandle;
	}

	public void show( ImageDisplay imageDisplay )
	{
		registerSourceDisplay( imageDisplay );

		// open
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		for ( String sourceName : imageDisplay.getSources() )
		{
			final ImageSource source = moBIE2.getSource( sourceName );
			new Thread( () -> Logger.log( "Opening: " + sourceName ) ).start();
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );
			sourceAndConverters.add( sourceAndConverter );
		}

		// transform
		sourceAndConverters = transformSourceAndConverters( sourceAndConverters, imageDisplay.sourceTransformers );

		// show
		List< SourceAndConverter< ? > > displayedSourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			// replace converter such that one can change the opacity
			// (this changes the hash-code of the sourceAndConverter)

			// TODO: understand this madness
			final Converter< RealType, ARGBType > converter = ( Converter< RealType, ARGBType > ) sourceAndConverter.getConverter();
			final Converter< ? extends Volatile< ? >, ARGBType > volatileConverter = sourceAndConverter.asVolatile().getConverter();
			sourceAndConverter = new ConverterChanger( sourceAndConverter, new AdjustableOpacityColorConverter(  converter ), new VolatileAdjustableOpacityColorConverter( volatileConverter ) ).get();

			// adapt color
			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplay.getColor() ) ).run();

			// set blending mode
			if ( imageDisplay.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, imageDisplay.getBlendingMode());

			// show
			displayService.show( bdvHandle, sourceAndConverter );

			// adapt contrast limits
			final ConverterSetup converterSetup = displayService.getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( imageDisplay.getContrastLimits()[ 0 ], imageDisplay.getContrastLimits()[ 1 ] );

			displayedSourceAndConverters.add( sourceAndConverter );
		}

		sacService.getUI().hide();

		imageDisplay.sourceAndConverters = displayedSourceAndConverters;
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

	private void registerSourceDisplay( SourceDisplay imageDisplay )
	{
		imageDisplay.sliceViewer = this;
		sourceDisplays.add( imageDisplay );
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
