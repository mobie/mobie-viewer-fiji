package de.embl.cba.mobie.bdv;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.color.NonSelectedSegmentsOpacityAdjusterCommand;
import de.embl.cba.mobie.segment.BdvSegmentSelector;
import de.embl.cba.mobie.color.RandomColorSeedChangerCommand;
import de.embl.cba.mobie.view.ViewerManager;
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
	public static final String UNDO_SEGMENT_SELECTIONS = "Undo segment selections [ Ctrl Shift N ]";
	public static final String CHANGE_RANDOM_COLOR_SEED = "Change random color seed";
	public static final String LOAD_ADDITIONAL_VIEWS = "Load additional views";
	public static final String SAVE_CURRENT_SETTINGS_AS_VIEW = "Save current settings as view";
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

		installContextMenuAndKeyboardShortCuts();
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

	private void installContextMenuAndKeyboardShortCuts( )
	{
		final BdvSegmentSelector segmentBdvSelector = new BdvSegmentSelector( bdvHandle, is2D, () -> viewerManager.getSegmentationDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			segmentBdvSelector.clearSelection();
		} );

		sacService.registerAction( sacService.getCommandName( RandomColorSeedChangerCommand.class ), sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			RandomColorSeedChangerCommand.incrementRandomColorSeed( sourceAndConverters );
		} );

		sacService.registerScijavaCommand( NonSelectedSegmentsOpacityAdjusterCommand.class );

		sacService.registerAction( LOAD_ADDITIONAL_VIEWS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewerManager.getAdditionalViewsLoader().loadAdditionalViewsDialog();
		} );

		sacService.registerAction( SAVE_CURRENT_SETTINGS_AS_VIEW, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewerManager.getViewsSaver().saveCurrentSettingsAsViewDialog();
		} );

		final Set< String > actionsKeys = sacService.getActionsKeys();

		final String[] actions = {
				sacService.getCommandName( ScreenShotMakerCommand.class ),
				sacService.getCommandName( BdvLocationLogger.class ),
				sacService.getCommandName( SourceAndConverterBlendingModeChangerCommand.class ),
				sacService.getCommandName( RandomColorSeedChangerCommand.class ),
				sacService.getCommandName( NonSelectedSegmentsOpacityAdjusterCommand.class ),
				UNDO_SEGMENT_SELECTIONS,
				LOAD_ADDITIONAL_VIEWS,
				SAVE_CURRENT_SETTINGS_AS_VIEW
		};

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions );

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> segmentBdvSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () ->
						{
							segmentBdvSelector.clearSelection();
						}).start(),
				"Clear selection", "ctrl shift N" ) ;
	}

	public SourceAndConverterService getSacService()
	{
		return sacService;
	}

	public SourceAndConverterContextMenuClickBehaviour getContextMenu()
	{
		return contextMenu;
	}

	private BdvHandle createBdv( int numTimepoints )
	{
		// create Bdv
		final MinimalBdvCreator bdvCreator = new MinimalBdvCreator( "MoBIE", is2D, Projector.MIXED_PROJECTOR, true, numTimepoints );
		final BdvHandle bdvHandle = bdvCreator.get();

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
