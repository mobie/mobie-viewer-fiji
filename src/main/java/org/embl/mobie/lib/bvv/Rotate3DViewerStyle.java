package org.embl.mobie.lib.bvv;

import net.imglib2.realtransform.AffineTransform3D;

import org.scijava.ui.behaviour.DragBehaviour;

import bvvpg.vistools.BvvHandle;

public class Rotate3DViewerStyle implements DragBehaviour
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

	public Rotate3DViewerStyle( final double speed, BvvHandle bvvHandle_)
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
