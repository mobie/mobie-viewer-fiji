/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.command.context.*;
import org.embl.mobie.command.context.CurrentLocationLoggerCommand;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.Services;
import org.embl.mobie.lib.annotation.SliceViewAnnotationSelector;
import org.embl.mobie.lib.bdv.*;
import org.embl.mobie.lib.bdv.blend.AccumulateAlphaBlendingProjectorARGB;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.bdv.overlay.ImageNameOverlay;
import org.embl.mobie.lib.color.OpacityHelper;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.AbstractDisplay;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.ui.WindowArrangementHelper;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.RunnableAction;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SliceViewer
{
	public static final String UNDO_SEGMENT_SELECTIONS = "Undo Segment Selections [ Ctrl Shift N ]";
	public static final String LOAD_ADDITIONAL_VIEWS = "Load Additional Views";
	public static final String SAVE_CURRENT_SETTINGS_AS_VIEW = "Save Current View";
	public static final String DELETE_VIEW = "Delete View";
	public static final String FRAME_TITLE = "MoBIE BigDataViewer";
	public static boolean tileRenderOverlay = false;
	private final SourceAndConverterBdvDisplayService bdvDisplayService;
	private BdvHandle bdvHandle;
	private final MoBIE moBIE;
	private final boolean is2D;
	private final ArrayList< String > projectCommands;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;
	private final ImageNameOverlay imageNameOverlay;

	public SliceViewer( MoBIE moBIE, boolean is2D )
	{
		this.moBIE = moBIE;
		this.is2D = is2D;
		this.projectCommands = moBIE.getProjectCommands();

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		bdvDisplayService = SourceAndConverterServices.getBdvDisplayService();

		bdvHandle = getBdvHandle();

		if ( tileRenderOverlay )
		{
			bdvHandle.getViewerPanel().showDebugTileOverlay();
			tileRenderOverlay = false; // don't show twice
		}

		imageNameOverlay = new ImageNameOverlay( this );

		installContextMenuAndKeyboardShortCuts();

		WindowArrangementHelper.rightAlignWindow( moBIE.getUserInterface().getWindow(), SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() ), true, true );
	}

	public ImageNameOverlay getImageNameOverlay()
	{
		return imageNameOverlay;
	}

	public synchronized BdvHandle getBdvHandle()
	{
		if ( bdvHandle == null )
		{
			bdvHandle = createBdv( is2D, FRAME_TITLE );
			bdvDisplayService.registerBdvHandle( bdvHandle );
			AccumulateAlphaBlendingProjectorARGB.bdvHandle = bdvHandle;
		}

		return bdvHandle;
	}

	private void installContextMenuAndKeyboardShortCuts( )
	{
		final SliceViewAnnotationSelector sliceViewAnnotationSelector =
				new SliceViewAnnotationSelector( bdvHandle, is2D, () -> moBIE.getViewManager().getAnnotationDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			sliceViewAnnotationSelector.clearSelection();
		} );

		sacService.registerAction( LOAD_ADDITIONAL_VIEWS, sourceAndConverters -> {
			moBIE.getViewManager().getAdditionalViewsLoader().loadAdditionalViewsDialog();
		} );

		sacService.registerAction( SAVE_CURRENT_SETTINGS_AS_VIEW, sourceAndConverters -> {
			View view = moBIE.getViewManager().createViewFromCurrentState();
			moBIE.getViewManager().getViewsSaver().saveViewDialog( view );
		} );

		sacService.registerAction( DELETE_VIEW, sourceAndConverters -> {
			moBIE.getViewManager().getViewsDeleter().deleteViewDialog();
		});

		final Set< String > actionsKeys = sacService.getActionsKeys();
		final ArrayList< String > actions = new ArrayList< String >();
		actions.add( SourceAndConverterService.getCommandName( SourcesInfoCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( CurrentLocationLoggerCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( ScreenShotMakerCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( ScreenShotStackMakerCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( ShowRawImagesCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( BoxSelectionCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( BigWarpRegistrationCommand.class ) );
		//actions.add( SourceAndConverterService.getCommandName( AutomaticRegistrationCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( ManualTransformationCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( EnterTransformationCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( FlipCommand.class ) );
		actions.add( SourceAndConverterService.getCommandName( ConfigureBVVRenderingCommand.class ) );
		actions.add( UNDO_SEGMENT_SELECTIONS );
		actions.add( LOAD_ADDITIONAL_VIEWS );
		actions.add( SAVE_CURRENT_SETTINGS_AS_VIEW );
		actions.add( DELETE_VIEW );

		if ( projectCommands != null )
		{
			actions.addAll( projectCommands );
		}

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions.toArray( new String[0] ) );

		// Install keyboard shortcuts

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );

		ActionMap actionMap = new ActionMap();
		actionMap.put( "MoBIE manual transform", new RunnableAction( "MoBIE manual transform",
				() -> new Thread( () -> { Services.commandService.run( ManualTransformationCommand.class, true ); }).start() ) );
		InputMap inputMap = new InputMap();
		inputMap.put( KeyStroke.getKeyStroke("T"),"MoBIE manual transform" );
		bdvHandle.getKeybindings().addInputMap( "MoBIE", inputMap );
		bdvHandle.getKeybindings().addActionMap( "MoBIE", actionMap );

		// TODO: several of the below "behaviours" could also be just "actions"
		//       and thereby be added to the above inputMap and actionMap
		//       see: https://forum.image.sc/t/how-to-replace-a-key-mapping-in-bdv/98432/6
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewAnnotationSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewAnnotationSelector.clearSelection() ).start(),
				"Clear selection", "ctrl shift N" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> {
							final SourceAndConverter[] sourceAndConverters = sacService.getSourceAndConverters().toArray( new SourceAndConverter[ 0 ] );
							ConfigureLabelRenderingCommand.incrementRandomColorSeed( sourceAndConverters, bdvHandle );
						}).start(),
				"Change random color seed", "ctrl L" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> {
							CurrentLocationLoggerCommand.logCurrentPosition(
									bdvHandle,
									MoBIEHelper.getWindowCentreInCalibratedUnits( bdvHandle ),
									new CalibratedMousePositionProvider( bdvHandle ).getPositionAsDoubles()
									);
						}).start(),
				"Log current position", "C" ) ;
	}

	public static BdvHandle createBdv( boolean is2D, String frameTitle )
	{
		final MobieSerializableBdvOptions sOptions = new MobieSerializableBdvOptions();
		sOptions.is2D = is2D;
		sOptions.frameTitle = frameTitle;
		sOptions.interpolate = false;
		IJ.log("BigDataViewer (BDV) initialised");
		IJ.log("BDV navigation mode: " + ( is2D ? "2D" : "3D" ));
		IJ.log("BDV interpolation: Nearest neighbour");
		IJ.log("Use [I] keyboard shortcut in BDV window to change the interpolation mode" );
		IJ.log("" );

		IBdvSupplier bdvSupplier = new MobieBdvSupplier( sOptions );
		//SourceAndConverterServices.getBdvDisplayService().setDefaultBdvSupplier( bdvSupplier );
		//BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();
		return bdvSupplier.get();
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

	public void show( Image< ? > image, SourceAndConverter< ? > sourceAndConverter, AbstractDisplay< ? > display )
	{
		try
		{
			// register sac
			SourceAndConverterServices.getSourceAndConverterService().register( sourceAndConverter );

			// link sac to image
			DataStore.sourceToImage().forcePut( sourceAndConverter, image );

			// blending mode
			SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.class.getName(), display.getBlendingMode() );

			// time added (for alpha blending)
			SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.TIME_ADDED, System.currentTimeMillis() );

			// opacity
			OpacityHelper.setOpacity( sourceAndConverter, display.getOpacity() );

			// show in Bdv
			SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, display.isVisible(), sourceAndConverter );

			updateTimepointSlider();
		}
		catch ( Exception e )
		{
			System.err.println("There was an error when trying to show " + image.getName() );
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	public void updateTimepointSlider()
	{
		if ( bdvHandle == null ) return;
		if ( bdvHandle.getViewerPanel() == null ) return;
		if ( bdvHandle.getViewerPanel().state()	 == null ) return;

		final List< SourceAndConverter< ? > > sacs = bdvHandle.getViewerPanel().state().getSources();
		if ( sacs.isEmpty() ) return;

		int maxNumTimePoints = 1;
		for ( SourceAndConverter< ? > sac : sacs )
		{
			final Image< ? > image = DataStore.sourceToImage().get( sac );
			if ( image instanceof RegionAnnotationImage )
				continue; // https://github.com/mobie/mobie-viewer-fiji/issues/975

			int numTimePoints = SourceHelper.getNumTimePoints( sac.getSpimSource() );
			if ( numTimePoints > maxNumTimePoints ) maxNumTimePoints = numTimePoints;
		}
		bdvHandle.getViewerPanel().state().setNumTimepoints( maxNumTimePoints );
	}

}
