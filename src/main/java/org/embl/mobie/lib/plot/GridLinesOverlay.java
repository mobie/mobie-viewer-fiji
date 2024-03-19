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
package org.embl.mobie.lib.plot;


import bdv.util.BdvHandle;
import bdv.util.BdvOverlay;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;

public class GridLinesOverlay extends BdvOverlay
{
	public static final String NONE = "None";
	public static final String Y_NX = "y = n * x";
	public static final String Y_N = "y = n";

	private final BdvHandle bdvHandle;
	private final String columnNameX;
	private final String columnNameY;
	private final Interval scatterPlotInterval;
	private final long dataMaxValue;
	private final String lineOverlay;
	private int axisLabelsFontSize;

	// TODO: make an own overlay for the axis labels (columnNameX, columnNameY)
	public GridLinesOverlay( BdvHandle bdvHandle, String columnNameX, String columnNameY, Interval plotInterval, String lineOverlay, int axisLabelsFontSize )
	{
		super();
		this.bdvHandle = bdvHandle;
		this.columnNameX = columnNameX;
		this.columnNameY = columnNameY;
		this.scatterPlotInterval = plotInterval;
		dataMaxValue = Math.max( plotInterval.max( 0 ), plotInterval.max( 1 ) );
		this.lineOverlay = lineOverlay;
		this.axisLabelsFontSize = axisLabelsFontSize;
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		g.setColor( Color.WHITE );

		final AffineTransform3D globalToViewerTransform = new AffineTransform3D();
		getCurrentTransform3D( globalToViewerTransform );

		drawGridLines( g, globalToViewerTransform );
		drawAxisLabels( g );
	}

	private void drawAxisLabels( Graphics2D g )
	{
		int fontSize = axisLabelsFontSize;

		g.setFont( new Font("MonoSpaced", Font.PLAIN, fontSize ) );

		int bdvWindowHeight = bdvHandle.getViewerPanel().getDisplay().getHeight();
		int bdvWindowWidth = bdvHandle.getViewerPanel().getDisplay().getWidth();

		int distanceToWindowBottom = 2 * ( fontSize + 5 );

		g.drawString( "y: " + columnNameY,
				bdvWindowWidth / 3,
				bdvWindowHeight - distanceToWindowBottom  );

		distanceToWindowBottom = 1 * ( fontSize + 5 );

		g.drawString( "x: " + columnNameX,
				bdvWindowWidth / 3,
				bdvWindowHeight - distanceToWindowBottom );
	}

	private void drawGridLines( Graphics2D g, AffineTransform3D globalToViewerTransform )
	{
		double[] zero = new double[ 3 ];
		globalToViewerTransform.apply( new double[ 3 ], zero );

		g.setColor( Color.WHITE );

		if ( lineOverlay.equals( Y_NX ) )
		{
			double[] n = new double[ 3 ];

			for ( int i = 0; i < 10; i++ )
			{
				globalToViewerTransform.apply( new double[]{ this.dataMaxValue, i * this.dataMaxValue, 0 }, n );
				g.drawLine( ( int ) zero[ 0 ], ( int ) zero[ 1 ],
						( int ) n[ 0 ], ( int ) n[ 1 ] );
			}
		}
		else if ( lineOverlay.equals( Y_N ) )
		{
			double[] max = new double[ 3 ];
			globalToViewerTransform.apply( new double[]{ this.dataMaxValue, this.dataMaxValue, 0 }, max );

			for ( int i = 0; i < 1000; i++ )
			{
				double[] n = new double[ 3 ];
				globalToViewerTransform.apply( new double[]{ 0, i, 0 }, n );

				g.drawLine(
						( int ) zero[ 0 ], ( int ) n[ 1 ],
						( int ) max[ 0 ], ( int ) n[ 1 ] );
			}
		}
	}

	private Font getAdaptedSizeFont( Graphics2D g, int i, String wellName, int fontSize )
	{
		Font font;
		final int stringWidth = g.getFontMetrics().stringWidth( wellName );
		if ( stringWidth > i )
		{
			fontSize *= 0.8 * i / stringWidth;
		}

		font = new Font( "TimesRoman", Font.PLAIN, fontSize );
		return font;
	}
}

