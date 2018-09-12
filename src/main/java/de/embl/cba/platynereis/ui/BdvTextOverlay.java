package de.embl.cba.platynereis.ui;

import bdv.util.*;
import net.imglib2.realtransform.AffineTransform2D;

import java.awt.*;
import java.util.ArrayList;

public class BdvTextOverlay extends BdvOverlay
{

	final String text;
	final double[] position;
	final int textSize;
	final Bdv bdv;
	private final int numDimensions;
	private final BdvOverlaySource< BdvTextOverlay > overlay;

	public BdvTextOverlay( Bdv bdv, String text, double[] position )
	{
		super();
		this.text = text;
		this.position = position;
		this.bdv = bdv;
		this.numDimensions = position.length;
		this.textSize = 200;

		overlay = BdvFunctions.showOverlay( this, "overlay", BdvOptions.options().addTo( bdv ) );

	}

	public void removeFromBdv()
	{
		overlay.removeFromBdv();
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform2D t = new AffineTransform2D();

		getCurrentTransform2D( t );

		final double scale = t.get( 0, 0 );

		double[] center = new double[ numDimensions ];

		t.apply( position, center );

		g.setColor( Color.MAGENTA );

		final FontMetrics fontMetrics = setFont( g, textSize );

		int[] stringPosition = getStringPosition( text, center, fontMetrics );

		g.drawString( text, stringPosition[ 0 ], stringPosition[ 1 ] );

	}

	private int[] getStringPosition( String name, double[] center, FontMetrics fontMetrics )
	{
		int[] stringSize = getStringSize( name, fontMetrics );

		int[] stringPosition = new int[ numDimensions ];
		for ( int d = 0; d < numDimensions; ++d )
		{
			stringPosition[ d ] = ( int ) ( center[ d ] - 0.5 * stringSize[ d ] );
		}
		return stringPosition;
	}

	private int[] getStringSize( String name, FontMetrics fontMetrics )
	{
		int[] graphicsSize = new int[ numDimensions ];
		graphicsSize[ 0 ] = fontMetrics.stringWidth( name );
		graphicsSize[ 1 ] = fontMetrics.getHeight();
		return graphicsSize;
	}

	private FontMetrics setFont( Graphics2D g, int fontSize )
	{
		g.setFont( new Font("TimesRoman", Font.PLAIN, fontSize ) );
		return g.getFontMetrics( g.getFont() );
	}

}
