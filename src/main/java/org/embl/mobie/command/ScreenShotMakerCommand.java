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
package org.embl.mobie.command;

import bdv.util.BdvHandle;
import ij.IJ;
import org.embl.mobie.lib.MoBIE;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Take Screenshot")
public class ScreenShotMakerCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{
    public static final String CAPTURE_SIZE_PIXELS = "Capture size [pixels]: ";

    @Parameter
    public BdvHandle bdvh;

    @Parameter(label="Sampling (in below units)", callback = "showNumPixels", min = "0.0", style="format:#.00000", stepSize = "0.01")
    public Double targetSamplingInXY = 1D;

    @Parameter(label="Pixel unit", choices = {"micrometer"} )
    public String pixelUnit;

    @Parameter(label="Show RGB Image")
    public boolean showRGB = true;

    @Parameter(label="Show Multi-Channel Image")
    public boolean showMultiChannel = true;


    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvh );
        screenShotMaker.setPhysicalPixelSpacingInXY( targetSamplingInXY, pixelUnit );

        if( showRGB )
            screenShotMaker.getRgbScreenShot().show();

        if( showMultiChannel )
            screenShotMaker.getRawScreenShot().show();

        if ( MoBIE.openedFromCLI )
            MoBIE.imageJ.ui().showUI();
    }

    @Override
    public void initialize() {

        // set pixel unit choices
        //
        final MutableModuleItem< String > pixelUnitItem = //
                getInfo().getMutableInput("pixelUnit", String.class);
        String pixelUnit = bdvh.getViewerPanel().state().getCurrentSource().getSpimSource().getVoxelDimensions().unit();
        final ArrayList< String > units = new ArrayList<>();
        units.add( pixelUnit );
        pixelUnitItem.setChoices( units );
    }

    // callback
    private void showNumPixels()
    {
        final long[] sizeInPixels = ScreenShotMaker.getCaptureImageSizeInPixels( bdvh, targetSamplingInXY );
        IJ.log( CAPTURE_SIZE_PIXELS + Arrays.toString( sizeInPixels ) );
//        final MutableModuleItem< String > message = getInfo().getMutableInput("message", String.class);
//        message.setValue( this, CAPTURE_SIZE_PIXELS + sizeInPixels[ 0 ] + ", " + sizeInPixels[ 1 ] );
    }
}
