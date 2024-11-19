package org.embl.mobie.lib.bvv;

import net.imglib2.realtransform.AffineTransform3D;

import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bvvpg.vistools.Bvv;
import bvvpg.vistools.BvvFunctions;
import bvvpg.vistools.BvvHandle;

public class BVVManager
{
	private Bvv bvv;
	
	//BVV canvas rendering parameters, can be changed/adjusted somewhere else
	
	//parameters that can be changed at runtime
	double dCam = 2000.;	
	double dClipNear = 1000.;
	double dClipFar = 15000.;			
	
	// parameters that require bvv restart, 
	// see https://github.com/ekatrukha/BigTrace/wiki/Volume-Render-Settings
	int renderWidth = 800;
	int renderHeight = 600;
	int numDitherSamples = 3; 
	int cacheBlockSize = 32;
	int maxCacheSizeInMB = 500;
	int ditherWidth = 3;
	
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
