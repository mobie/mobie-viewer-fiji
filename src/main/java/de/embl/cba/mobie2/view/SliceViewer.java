package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie2.bdv.BdvLocationLogger;
import de.embl.cba.mobie2.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.mobie2.segment.BdvSegmentSelector;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.MinimalBdvCreator;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourceAndConverterBlendingModeChangerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.Supplier;

public class SliceViewer implements Supplier< BdvHandle >
{
	private final SourceAndConverterBdvDisplayService sacDisplayService;
	private BdvHandle bdvHandle;
	private final boolean is2D;
	private final ViewerManager viewerManager;
	private final int timepoints;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;

	public SliceViewer( boolean is2D, ViewerManager viewerManager, int timepoints )
	{
		this.is2D = is2D;
		this.viewerManager = viewerManager;
		this.timepoints = timepoints;

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		sacDisplayService = SourceAndConverterServices.getSourceAndConverterDisplayService();

		bdvHandle = createBdv( timepoints );
		sacDisplayService.registerBdvHandle( bdvHandle );

		installContextMenu();
	}

	@Override
	public BdvHandle get()
	{
		if ( bdvHandle == null )
		{
			bdvHandle = createBdv( timepoints );
			sacDisplayService.registerBdvHandle( bdvHandle );
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
