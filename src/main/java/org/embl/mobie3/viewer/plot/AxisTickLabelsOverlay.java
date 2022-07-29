/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie3.viewer.plot;


import bdv.util.BdvOverlay;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform2D;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

public class AxisTickLabelsOverlay extends BdvOverlay
{
	private final HashMap< String, Double > xLabelToIndex;
	private final HashMap< String, Double > yLabelToIndex;
	private final FinalRealInterval dataInterval;
	private final double xMin;
	private final double yMin;
	private int offset;
	private int fontSize;

	public AxisTickLabelsOverlay( HashMap< String, Double > xLabelToIndex, HashMap< String, Double > yLabelToIndex, FinalRealInterval dataInterval )
	{
		super();
		this.xLabelToIndex = xLabelToIndex;
		this.yLabelToIndex = yLabelToIndex;
		this.dataInterval = dataInterval;
		xMin = dataInterval.realMin( 0 );
		yMin = dataInterval.realMin( 1 );
		offset = 10;
		fontSize = 15;
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform2D viewerTransform = new AffineTransform2D();
		this.getCurrentTransform2D( viewerTransform );
		g.setColor( Color.WHITE );

		g.setFont( new Font( "MonoSpaced", Font.PLAIN, fontSize ) );

		AffineTransform orig = g.getTransform();
		//g.rotate(-Math.PI/2);

		final float[] globalLocation = new float[ 2 ];
		final float[] viewerLocation = new float[ 2 ];

		// draw x axis tick marks
		globalLocation[ 1 ] = (float) yMin;
		for ( Map.Entry< String, Double > entry : xLabelToIndex.entrySet() )
		{
			globalLocation[ 0 ] = entry.getValue().floatValue();
			viewerTransform.apply( globalLocation, viewerLocation );
			float xPos = viewerLocation[ 0 ];
			float yPos = viewerLocation[ 1 ];
			final String text = entry.getKey();
			final int stringWidth = g.getFontMetrics().stringWidth( text );
			yPos = yPos - stringWidth + offset;
			g.setTransform( orig );
			g.rotate(-Math.PI/2, xPos, yPos );
			g.drawString( text, xPos, yPos );
		}

		// draw y axis tick marks
		g.setTransform( orig );
		globalLocation[ 0 ] = (float) xMin;
		for ( Map.Entry< String, Double > entry : yLabelToIndex.entrySet() )
		{
			globalLocation[ 1 ] = entry.getValue().floatValue();
			viewerTransform.apply( globalLocation, viewerLocation );
			final String text = entry.getKey();
			final int stringWidth = g.getFontMetrics().stringWidth( text );
			float xPos = viewerLocation[ 0 ];
			xPos = xPos - stringWidth - offset;
			g.drawString( entry.getKey(), xPos, viewerLocation[ 1 ]);
		}
	}
}

