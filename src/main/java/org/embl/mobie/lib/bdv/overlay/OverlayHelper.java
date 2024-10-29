package org.embl.mobie.lib.bdv.overlay;

import net.imglib2.FinalRealInterval;

import java.awt.*;

public class OverlayHelper
{
    public static void drawTextWithBackground( Graphics2D g, OverlayTextItem item )
    {
        // draw background (this helps with https://github.com/mobie/mobie-viewer-fiji/issues/1013)
        // TODO in addition, one could also determine all the text bounds as an Interval
        //   and then only draw the names that don't overlap with something that has been drawn already
        g.setColor( Color.BLACK );
        g.fillRect(
                item.x,
                item.y - item.height + g.getFontMetrics().getDescent(),
                item.width,
                item.height );

        // draw text
        g.setColor( Color.WHITE );
        g.drawString( item.text, item.x, item.y );
    }

    public static OverlayTextItem itemFromBounds(
            Graphics2D g,
            FinalRealInterval bounds,
            String text,
            final Font font )
    {
        final double width = bounds.realMax( 0 ) - bounds.realMin( 0 );
        final double center = ( bounds.realMax( 0 ) + bounds.realMin( 0 ) ) / 2.0;

        g.setFont( font );
        final float computedFontSize = ( float ) ( 1.0F * ImageNameOverlay.MAX_FONT_SIZE * width / ( 1.0F * g.getFontMetrics().stringWidth( text ) ) );
        final float finalFontSize = Math.min( ImageNameOverlay.MAX_FONT_SIZE, computedFontSize );
        Font finalFont = font.deriveFont( finalFontSize );
        g.setFont( finalFont );

        final OverlayTextItem item = new OverlayTextItem();
        item.text = text;
        item.width = g.getFontMetrics().stringWidth( text );
        item.height = g.getFontMetrics().getHeight();
        item.x = (int) ( center - item.width / 2.0 );
        item.y = (int) ( bounds.realMax( 1 ) + 1.1F * finalFont.getSize() );
        return item;
    }

}
