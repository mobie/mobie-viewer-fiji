package org.embl.mobie.lib.bvv;

import net.imglib2.realtransform.AffineTransform3D;

import org.embl.mobie.command.context.ConfigureBVVRenderingCommand;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModuleItem;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bvvpg.vistools.Bvv;
import bvvpg.vistools.BvvFunctions;
import bvvpg.vistools.BvvHandle;
import ch.epfl.biop.bdv.img.Services;

public class BVVManager
{
	private Bvv bvv;
	
	//BVV canvas rendering parameters, can be changed/adjusted somewhere else
	
	//parameters that can be changed at runtime
	static double dCam = 2000.;
	static double dClipNear = 1000.;
	static double dClipFar = 15000.;			
	
	// parameters that require bvv restart, 
	// see https://github.com/ekatrukha/BigTrace/wiki/Volume-Render-Settings
	static int renderWidth = 800;
	static int renderHeight = 600;
	static int numDitherSamples = 3; 
	static int cacheBlockSize = 32;
	static int maxCacheSizeInMB = 500;
	static int ditherWidth = 3;
	
	public BVVManager()
	{
		readBVVRenderSettings();
	}
	
	public synchronized Bvv get()
	{
		if ( bvv == null )
		{
			bvv = BvvFunctions.show( Bvv.options().frameTitle( "BigVolumeViewer" ).
					dCam(dCam).
					dClipNear(dClipNear).
					dClipFar(dClipFar).				
					renderWidth(renderWidth).
					renderHeight(renderHeight).
					numDitherSamples(numDitherSamples ).
					cacheBlockSize(cacheBlockSize ).
					maxCacheSizeInMB( maxCacheSizeInMB ).
					ditherWidth(ditherWidth)
					);
			this.bvv.getBvvHandle().getViewerPanel().state().getVisibleAndPresentSources();
			
			//change drag rotation for navigation "3D Viewer" style
			BvvHandle bvvHandle = bvv.getBvvHandle();
			final Rotate dragRotate = new Rotate( 0.75, bvvHandle);
			final Rotate dragRotateFast = new Rotate( 2.0, bvvHandle);
			final Rotate dragRotateSlow = new Rotate( 0.1, bvvHandle);
			
			Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
			behaviours.behaviour( dragRotate, "drag rotate", "button1" );
			behaviours.behaviour( dragRotateFast, "drag rotate fast", "shift button1" );
			behaviours.behaviour( dragRotateSlow, "drag rotate slow", "ctrl button1" );
			behaviours.install( bvvHandle.getTriggerbindings(), "my-new-behaviours" );
		}
		return bvv;
	}

	public void setBVV( Bvv bvv )
	{
		this.bvv = bvv;
	}

	public void close()
	{
		if ( bvv != null )
		{
			bvv.close();
		}
	}
	
	public void updateBVVRenderSettings()
	{
		readBVVRenderSettings();
		if (bvv != null)
		{
			bvv.getBvvHandle().getViewerPanel().setCamParams( dCam, dClipNear, dClipFar );
			bvv.getBvvHandle().getViewerPanel().requestRepaint();
		}
	}
	
	/** Reads current BVV rendering settings from the corresponding SciJava command.
	 * Returns true if new seetings require BVV restart **/
	@SuppressWarnings( "unchecked" )
	boolean readBVVRenderSettings()
	{
		
		boolean bRestartBVV = false;

		final CommandService cs = Services.commandService;
		
		final ModuleService ms = Services.commandService.moduleService();
		
		final CommandInfo info = cs.getCommand( ConfigureBVVRenderingCommand.class );
		
		dCam = Math.abs((double)getSettingsValue(info, ms, "dCam"));
		
		dClipFar = Math.abs((double)getSettingsValue(info, ms, "dClipFar"));

		dClipNear = Math.abs((double)getSettingsValue(info, ms, "dClipNear"));

		//dClipNear should be less than dCam
		if(dCam < dClipNear)
		{
			dCam = dClipNear + 5.0;
			ms.save( (ModuleItem<Double>)info.getInput( "dCam" ), dCam );
		}
		
		int nTempInt = 	(int)getSettingsValue(info, ms, "renderWidth");		
		if(renderWidth != nTempInt)
		{
			bRestartBVV = true;
		}
		renderWidth = nTempInt;
		
		nTempInt = 	(int)getSettingsValue(info, ms, "renderHeight");		
		if(renderHeight != nTempInt)
		{
			bRestartBVV = true;
		}
		renderHeight = nTempInt;

		nTempInt = 	(int)getSettingsValue(info, ms, "numDitherSamples");		
		if(numDitherSamples != nTempInt)
		{
			bRestartBVV = true;
		}
		numDitherSamples = nTempInt;
		
		nTempInt = 	(int)getSettingsValue(info, ms, "cacheBlockSize");		
		if(cacheBlockSize != nTempInt)
		{
			bRestartBVV = true;
		}
		cacheBlockSize = nTempInt;
		
		nTempInt = 	(int)getSettingsValue(info, ms, "maxCacheSizeInMB");		
		if(maxCacheSizeInMB != nTempInt)
		{
			bRestartBVV = true;
		}
		maxCacheSizeInMB = nTempInt;
		
		String dithering = ( String ) getSettingsValue(info, ms, "dithering");
		final int ditherWidthIn;
		switch ( dithering )
		{
		case "none (always render full resolution)":
		default:
			ditherWidthIn = 1;
			break;
		case "2x2":
			ditherWidthIn = 2;
			break;
		case "3x3":
			ditherWidthIn = 3;
			break;
		case "4x4":
			ditherWidthIn = 4;
			break;
		case "5x5":
			ditherWidthIn = 5;
			break;
		case "6x6":
			ditherWidthIn = 6;
			break;
		case "7x7":
			ditherWidthIn = 7;
			break;
		case "8x8":
			ditherWidthIn = 8;
			break;
		}
		
		if(ditherWidth != ditherWidthIn)
		{
			bRestartBVV = true;
		}
		
		ditherWidth = ditherWidthIn;
					
		return bRestartBVV;
	}
	
	
	@SuppressWarnings( "unchecked" )
	public <T> T getSettingsValue( final CommandInfo info, final ModuleService ms, String name) 
	{

		CommandModuleItem< T > item = ( CommandModuleItem< T > ) info.getInput( name);
		if(ms.load( item ) == null)
		{
			return ms.getDefaultValue( item );				
		}
		return ms.load( item );
	}
	
	/** 3D viewer-style rotation **/
	private class Rotate implements DragBehaviour
	{
		/**
		 * Coordinates where mouse dragging started.
		 */
		private double oX, oY;
		
		private final double speed;
		/**
		 * One step of rotation (radian).
		 */
		final private static double step = Math.PI / 180;
		
		private int centerX = 0, centerY = 0;
		final BvvHandle bvvHandle;
		private final AffineTransform3D transform = new AffineTransform3D();
		private final AffineTransform3D affineDragStart = new AffineTransform3D();
		private final AffineTransform3D affineDragCurrent = new AffineTransform3D();

		public Rotate( final double speed, BvvHandle bvvHandle_)
		{		
			this.bvvHandle = bvvHandle_;
			this.speed = speed;
		}

		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			centerX = bvvHandle.getViewerPanel().getDisplay().getWidth()/2;
			centerY = bvvHandle.getViewerPanel().getDisplay().getHeight()/2;
			//affineDragStart.set(bvvHandle.getViewerPanel().state().getViewerTransform());
			//transform.get( affineDragStart );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final double dX = oX - x;
			final double dY = oY - y;

			affineDragCurrent.set( affineDragStart );

			// center shift
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) - centerX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) - centerY, 1, 3 );
			final double v = step * speed;
			affineDragCurrent.rotate( 0, -dY * v );
			affineDragCurrent.rotate( 1, dX * v );

			// center un-shift
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) + centerX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) + centerY, 1, 3 );
			
			//does not depend on how far we from initial click
			oX = x;
			oY = y;
			
			transform.set( bvvHandle.getViewerPanel().state().getViewerTransform() );
			transform.preConcatenate( affineDragCurrent );
			bvvHandle.getViewerPanel().state().setViewerTransform(transform);
		}

		@Override
		public void end( final int x, final int y )
		{}
	}
}
