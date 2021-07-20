package de.embl.cba.mobie.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.bdv.MobieBdvSupplier;
import de.embl.cba.mobie.bdv.MobieSerializableBdvOptions;
import de.embl.cba.mobie.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.mobie.bdv.ViewerTransformLogger;
import de.embl.cba.mobie.command.NonSelectedSegmentsOpacityAdjusterCommand;
import de.embl.cba.mobie.command.SegmentsVolumeRenderingConfiguratorCommand;
import de.embl.cba.mobie.command.SelectedSegmentsColorConfiguratorCommand;
import de.embl.cba.mobie.command.SourceAndConverterBlendingModeChangerCommand;
import de.embl.cba.mobie.segment.BdvSegmentSelector;
import de.embl.cba.mobie.command.RandomColorSeedChangerCommand;
import de.embl.cba.mobie.view.ViewerManager;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.source.ManualTransformCommand;
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
		final BdvSegmentSelector segmentBdvSelector = new BdvSegmentSelector( bdvHandle, is2D, () -> viewerManager.getSegmentationDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			segmentBdvSelector.clearSelection();
		} );

		sacService.registerAction( sacService.getCommandName( RandomColorSeedChangerCommand.class ), sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			RandomColorSeedChangerCommand.incrementRandomColorSeed( sourceAndConverters );
		} );

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
				//sacService.getCommandName( ScreenShotMakerCommand.class ),
				sacService.getCommandName( ViewerTransformLogger.class ),
				sacService.getCommandName( ManualTransformCommand.class ),
				sacService.getCommandName( SourceAndConverterBlendingModeChangerCommand.class ),
				sacService.getCommandName( RandomColorSeedChangerCommand.class ),
				sacService.getCommandName( NonSelectedSegmentsOpacityAdjusterCommand.class ),
				sacService.getCommandName( SelectedSegmentsColorConfiguratorCommand.class ),
				sacService.getCommandName( SegmentsVolumeRenderingConfiguratorCommand.class ),
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
