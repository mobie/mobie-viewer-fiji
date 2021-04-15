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

public class SliceViewer implements Supplier< BdvHandle >
{
	private final SourceAndConverterBdvDisplayService displayService;
	private BdvHandle bdvHandle;
	private final boolean is2D;
	private final ViewerManager< ?, ? > viewerManager;
	private final int timepoints;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;
	private List< SourceDisplay > sourceDisplays;

	public SliceViewer( boolean is2D, ViewerManager viewerManager, int timepoints )
	{
		this.is2D = is2D;
		this.viewerManager = viewerManager;
		this.timepoints = timepoints;

		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		sourceDisplays = new ArrayList<>();
		bdvHandle = createBdv( timepoints );
		displayService.registerBdvHandle( bdvHandle );

		// register context menu actions
		installContextMenu();
	}

	@Override
	public BdvHandle get()
	{
		if ( bdvHandle == null )
		{
			bdvHandle = createBdv( timepoints );
			displayService.registerBdvHandle( bdvHandle );
		}
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

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}
}
