package org.embl.mobie.lib.bdv.overlay;

import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;

import java.awt.*;

public class OverlayHelper
{
    public static void drawTextWithBackground( Graphics2D g, OverlayItem item, final boolean drawBackground )
    {
        // draw background (this helps with https://github.com/mobie/mobie-viewer-fiji/issues/1013)
        // but this is slow, if there are many annotations
        if ( drawBackground )
        {
            g.setColor( Color.BLACK );
            g.fillRect(
                    ( int ) item.interval.min( 0 ),
                    ( int ) item.interval.min( 1 ),
                    ( int ) item.interval.dimension( 0 ),
                    ( int ) item.interval.dimension( 1 ) );
        }

        // draw text
        g.setColor( Color.WHITE );
        g.setFont( item.font ); // <= this is slow
        g.drawString( item.text, item.x, item.y );

        int y = item.y - item.font.getSize() / 2;
        // g.drawLine(item.x - 25, y, item.x - 5, y); // https://github.com/mobie/mobie-viewer-fiji/issues/1246
    }

    public static OverlayItem itemFromBounds(
            Graphics2D g,
            FinalRealInterval bounds,
            String text,
            final Font font,
            int fontSize )
    {
        final double width = bounds.realMax( 0 ) - bounds.realMin( 0 );
        final double center = ( bounds.realMax( 0 ) + bounds.realMin( 0 ) ) / 2.0;

        g.setFont( font );

        if ( fontSize < 0 ) // adaptive font size
        {
            if ( g.getFontMetrics().stringWidth( text ) > width )
            {
                float adaptedFontSize = ( float ) ( font.getSize() * ( 1.0 * width / g.getFontMetrics().stringWidth( text ) ) );
                Font adaptedFont = font.deriveFont( adaptedFontSize );
                g.setFont( adaptedFont );
            }
        }

        final OverlayItem item = new OverlayItem();
        item.text = text;
        item.font = g.getFont();
        item.width = g.getFontMetrics().stringWidth( text );
        item.height = g.getFontMetrics().getHeight();
        item.x = (int) ( center - item.width / 2.0 );
        item.y = (int) ( bounds.realMax( 1 ) + 1.1F * item.font.getSize() );
        item.interval = FinalInterval.createMinSize(
                item.x,
                item.y - item.height + g.getFontMetrics().getDescent(),
                item.width,
                item.height );

        g.setFont( font ); // reset the original font

        return item;
    }

}
