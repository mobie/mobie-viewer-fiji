/*-
 * #%L
 * Fiji viewer for MoBIE projects
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
package org.embl.mobie.viewer.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.Prefs;
import bdv.viewer.Interpolation;
import bdv.viewer.OverlayRenderer;
import bdv.viewer.ViewerPanel;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;

import java.awt.*;

public class MobieBdvSupplier implements IBdvSupplier {

    public final MobieSerializableBdvOptions sOptions;

    public MobieBdvSupplier( MobieSerializableBdvOptions sOptions )
    {
        this.sOptions = sOptions;
    }

    @Override
    public BdvHandle get()
    {
        Prefs.showTextOverlay();
        Prefs.showScaleBar( true );
        Prefs.showMultibox( true );
        Prefs.sourceNameOverlayPosition( Prefs.OverlayPosition.TOP_RIGHT );

        BdvOptions options = sOptions.getBdvOptions();

        // create dummy image to instantiate the BDV
        ArrayImg< ByteType, ByteArray > dummyImg = ArrayImgs.bytes(2, 2, 2);
        options = options.sourceTransform( new AffineTransform3D() );

        BdvStackSource< ByteType > bss = BdvFunctions.show( dummyImg, "dummy", options );
        BdvHandle bdv = bss.getBdvHandle();

        if ( sOptions.interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        // remove dummy image
        bdv.getViewerPanel().state().removeSource( bdv.getViewerPanel().state().getCurrentSource() );

        // TODO: this constructions appears a bit brittle.
        //  BDV does not seem to handle it very well if there
        //  is no image shown...
        //  Maybe there is a better way to initialise an empty BDV?

        setTimepointTextColor( bdv );

        return bdv;
    }

    // This is a hack that could be removed once it is
    // fixed in BDV (discussion on bdv-core gitter with tpietzsch).
    private void setTimepointTextColor( BdvHandle bdv )
    {
        ViewerPanel viewer = bdv.getViewerPanel();
        viewer.getDisplay().overlays().add( 1, new OverlayRenderer()
        {
            @Override
            public void drawOverlays( Graphics g )
            {
                g.setColor( Color.WHITE );
            }

            @Override
            public void setCanvasSize( final int width, final int height ) {}
        } );
    }

}
