package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.MobieBdvSupplier;
import org.embl.mobie.viewer.bdv.MobieSerializableBdvOptions;
import org.embl.mobie.viewer.bdv.SourcesAtMousePositionSupplier;
import org.embl.mobie.viewer.bdv.ViewerTransformLogger;
import org.embl.mobie.viewer.command.MaskSourcesCommand;
import org.embl.mobie.viewer.command.ImagePlusExportCommand;
import org.embl.mobie.viewer.command.ManualRegistrationCommand;
import org.embl.mobie.viewer.command.BigWarpRegistrationCommand;
import org.embl.mobie.viewer.command.ImageVolumeRenderingConfiguratorCommand;
import org.embl.mobie.viewer.command.NonSelectedSegmentsOpacityAdjusterCommand;
import org.embl.mobie.viewer.command.SegmentsVolumeRenderingConfiguratorCommand;
import org.embl.mobie.viewer.command.SelectedSegmentsColorConfiguratorCommand;
import org.embl.mobie.viewer.command.SourceAndConverterBlendingModeChangerCommand;
import org.embl.mobie.viewer.command.ScreenShotMakerCommand;
import org.embl.mobie.viewer.segment.BdvSegmentSelector;
import org.embl.mobie.viewer.command.RandomColorSeedChangerCommand;
import org.embl.mobie.viewer.view.ViewManager;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.Supplier;

public class SliceViewer implements Supplier< BdvHandle >
{
	public static final String UNDO_SEGMENT_SELECTIONS = "Undo Segment Selections [ Ctrl Shift N ]";
	public static final String CHANGE_RANDOM_COLOR_SEED = "Change Random Color Seed";
	public static final String LOAD_ADDITIONAL_VIEWS = "Load Additional Views";
	public static final String SAVE_CURRENT_SETTINGS_AS_VIEW = "Save Current Settings As View";
	private final SourceAndConverterBdvDisplayService sacDisplayService;
	private BdvHandle bdvHandle;
	private final boolean is2D;
	private final ViewManager viewManager;
	private final int timepoints;
	private final ArrayList< String > projectCommands;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;

	public SliceViewer( boolean is2D, ViewManager viewManager, int timepoints, ArrayList< String > projectCommands )
	{
		this.is2D = is2D;
		this.viewManager = viewManager;
		this.timepoints = timepoints;
		this.projectCommands = projectCommands;

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		sacDisplayService = SourceAndConverterServices.getBdvDisplayService();

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
		final BdvSegmentSelector segmentBdvSelector = new BdvSegmentSelector( bdvHandle, is2D, () -> viewManager.getSegmentationDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			segmentBdvSelector.clearSelection();
		} );

		// TODO: why is this needed here (and not just in below list?)
//		sacService.registerAction( sacService.getCommandName( RandomColorSeedChangerCommand.class ), sourceAndConverters -> {
//			// TODO: Maybe only do this for the sacs at the mouse position
//			RandomColorSeedChangerCommand.incrementRandomColorSeed( sourceAndConverters );
//		} );

		sacService.registerAction( LOAD_ADDITIONAL_VIEWS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewManager.getAdditionalViewsLoader().loadAdditionalViewsDialog();
		} );

		sacService.registerAction( SAVE_CURRENT_SETTINGS_AS_VIEW, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewManager.getViewsSaver().saveCurrentSettingsAsViewDialog();
		} );

		final Set< String > actionsKeys = sacService.getActionsKeys();

		final ArrayList< String > actions = new ArrayList< String >();
		actions.add( sacService.getCommandName( ScreenShotMakerCommand.class ) );
		actions.add( sacService.getCommandName( ImagePlusExportCommand.class ) );
		actions.add( sacService.getCommandName( ViewerTransformLogger.class ) );
		actions.add( sacService.getCommandName( MaskSourcesCommand.class ) );
		actions.add( sacService.getCommandName( BigWarpRegistrationCommand.class ) );
		actions.add( sacService.getCommandName( ManualRegistrationCommand.class ) );
		actions.add( sacService.getCommandName( SourceAndConverterBlendingModeChangerCommand.class ) );
		actions.add( sacService.getCommandName( RandomColorSeedChangerCommand.class ) );
		actions.add( sacService.getCommandName( NonSelectedSegmentsOpacityAdjusterCommand.class ) );
		actions.add( sacService.getCommandName( SelectedSegmentsColorConfiguratorCommand.class ) );
		actions.add( sacService.getCommandName( SegmentsVolumeRenderingConfiguratorCommand.class ) );
		actions.add( sacService.getCommandName( ImageVolumeRenderingConfiguratorCommand.class ) );
		actions.add( UNDO_SEGMENT_SELECTIONS );
		actions.add( LOAD_ADDITIONAL_VIEWS );
		actions.add( SAVE_CURRENT_SETTINGS_AS_VIEW );

		if ( projectCommands != null )
		{
			for ( String commandName : projectCommands )
			{
				actions.add( commandName );
			}
		}

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions.toArray( new String[0] ) );

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

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () ->
						{
							final SourceAndConverter[] sourceAndConverters = sacService.getSourceAndConverters().toArray( new SourceAndConverter[ 0 ] );
							RandomColorSeedChangerCommand.incrementRandomColorSeed( sourceAndConverters );
						}).start(),
				"Change random color seed", "ctrl L" ) ;
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
		final MobieSerializableBdvOptions sOptions = new MobieSerializableBdvOptions();
		sOptions.is2D = is2D;
		sOptions.numTimePoints = numTimepoints;

		IBdvSupplier bdvSupplier = new MobieBdvSupplier( sOptions );

		SourceAndConverterServices.getBdvDisplayService().setDefaultBdvSupplier(bdvSupplier);

		BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

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
