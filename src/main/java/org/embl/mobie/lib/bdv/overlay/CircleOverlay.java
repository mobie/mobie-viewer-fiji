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
package org.embl.mobie.lib.bdv.overlay;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CircleOverlay extends BdvOverlay
{
	final List< RealPoint > points;
	private final double radius;

	public CircleOverlay( double[] location, double radius )
	{
		super();
		this.points = new ArrayList< RealPoint >();
		this.points.add( new RealPoint( location ) );
		this.radius = radius;
	}

	public CircleOverlay( RealPoint point, double radius )
	{
		super();
		this.points = new ArrayList< RealPoint >();
		this.points.add( point );
		this.radius = radius;
	}

	public CircleOverlay( List< RealPoint > points, double radius )
	{
		super();
		this.points = points;
		this.radius = radius;
	}

	public void addCircle( double[] point )
	{
		this.points.add( new RealPoint( point ) );
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		getCurrentTransform3D( transform );

		final double[] globalPosition = new double[ 3 ];
		final double[] viewPosition = new double[ 3 ];
		final double[] scale = new double[ 3 ];
		final int[] viewDiameter = new int[ 2 ];
		for ( final RealPoint point : points )
		{
			point.localize( globalPosition );
			transform.apply( globalPosition, viewPosition );
			for ( int d = 0; d < 3; d++ )
				scale[ d ] = Affine3DHelpers.extractScale( transform, d );

			final double depth = Math.abs( viewPosition[ 2 ] ) / scale[ 2 ];
			setViewDiameter( depth, scale, viewDiameter );
			if ( viewDiameter[ 0 ] == 0 || viewDiameter[ 1 ] == 0 )
				return;
			final int x = ( int ) ( viewPosition[ 0 ] - 0.5 * viewDiameter [ 0 ] );
			final int y = ( int ) ( viewPosition[ 1 ] - 0.5 * viewDiameter [ 1 ] );
			final Color color = new Color( 255, 0, 255, 150 );
			g.setColor( color );
			g.drawOval( x, y, viewDiameter[ 0 ], viewDiameter[ 1 ] );
		}
	}

	private void setViewDiameter( final double depth, double[] scaleXY, int[] viewerDiameter )
	{
		for ( int d = 0; d < 2; d++ )
		{
			if ( depth > radius )
				viewerDiameter[ d ] = 0;
			else
				viewerDiameter[ d ] = (int) Math.round( scaleXY[ d ] *  2 * Math.sqrt( radius * radius - depth * depth ) );
		}
	}
}
