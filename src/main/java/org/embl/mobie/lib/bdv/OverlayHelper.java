package org.embl.mobie.lib.bdv;

import net.imglib2.FinalRealInterval;
import org.embl.mobie.lib.annotation.Annotation;

import java.awt.*;

public class OverlayHelper
{
    public static void drawTextWithBackground( Graphics2D g, OverlayStringItem item )
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

    public static OverlayStringItem itemFromBounds(
            Graphics2D g,
            FinalRealInterval bounds,
            String text,
            final Font font )
    {
        OverlayStringItem item = new OverlayStringItem();
        item.text = text;

        final double width = bounds.realMax( 0 ) - bounds.realMin( 0 );
        final double center = ( bounds.realMax( 0 ) + bounds.realMin( 0 ) ) / 2.0;

        // font size
        g.setFont( font );
        final float computedFontSize = ( float ) ( 1.0F * ImageNameOverlay.MAX_FONT_SIZE * width / ( 1.0F * g.getFontMetrics().stringWidth( text ) ) );
        final float finalFontSize = Math.min( ImageNameOverlay.MAX_FONT_SIZE, computedFontSize );
        Font finalFont = font.deriveFont( finalFontSize );
        g.setFont( finalFont );

        // text width and height
        item.width = g.getFontMetrics().stringWidth( text );
        item.height = g.getFontMetrics().getHeight();

        // text coordinates
        item.x = (int) ( center - item.width / 2.0 );
        item.y = (int) ( bounds.realMax( 1 ) + 1.1F * finalFont.getSize() );
        return item;
    }

    public static < A extends Annotation > OverlayStringItem itemFromLocation(
            Graphics2D g,
            String text,
            double[] position,
            int numAnnotations,
            final Font font )
    {
        OverlayStringItem item = new OverlayStringItem();
        item.text = text;

        g.setFont( font );

        final float computedFontSize = ( float ) ( 3.0 * AnnotationOverlay.MAX_FONT_SIZE / Math.sqrt( numAnnotations ) );
        final float finalFontSize = Math.min ( AnnotationOverlay.MAX_FONT_SIZE, computedFontSize );

        Font finalFont = font.deriveFont( finalFontSize );
        g.setFont( finalFont );

        item.width = g.getFontMetrics().stringWidth( text );
        item.height = g.getFontMetrics().getHeight();

        item.x = (int) ( position[ 0 ] - item.width / 2 );
        item.y = (int) ( position[ 1 ] + 1.5 * item.height ); // paint a bit below (good for points)
        return item;
    }
}
