package de.embl.cba.platynereis.bdv;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class BdvPointOverlay extends BdvOverlay
{
	final List< RealPoint > points;
	private final double depthOfField;

	public BdvPointOverlay( double[] point, double depthOfField )
	{
		super();
		this.points = new ArrayList< RealPoint >();
		this.points.add( new RealPoint( point ) );
		this.depthOfField = depthOfField;
	}

	public BdvPointOverlay( RealPoint point, double depthOfField )
	{
		super();
		this.points = new ArrayList< RealPoint >();
		this.points.add( point );
		this.depthOfField = depthOfField;
	}

	public BdvPointOverlay( List< RealPoint > points, double depthOfField )
	{
		super();
		this.points = points;
		this.depthOfField = depthOfField;
	}

	public void addPoint( double[] point )
	{
		this.points.add( new RealPoint( point ) );
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform3D t = new AffineTransform3D();
		getCurrentTransform3D( t );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		for ( final RealPoint p : points )
		{
			p.localize( lPos );
			t.apply( lPos, gPos );
			final int size = getSize( gPos[ 2 ] );
			final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
			g.setColor( getColor( gPos[ 2 ] ) );
			g.fillOval( x, y, size, size );
		}
	}

	private Color getColor( final double depth )
	{
		int alpha = 200 - ( int ) Math.round( Math.abs( depth ) );

		if ( alpha < 150 )
			alpha = 150;

		return new Color( 255, 0, 255, alpha );
	}

	private int getSize( final double depth )
	{
		return ( int ) Math.max( 10, 30 - Math.round( Math.abs( depth ) / depthOfField  ) );
	}

}
