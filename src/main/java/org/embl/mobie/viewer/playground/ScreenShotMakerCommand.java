/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package org.embl.mobie.viewer.playground;

import bdv.util.BdvHandle;
import org.embl.mobie.viewer.bdv.ScreenShotMaker;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Screenshot",
        description = "Creates a screenshot of the current BDV view. The sampling can be chosen to upscale or downscale" +
                " the image compared to the current view. A single RGB image resulting from the projection" +
                " of all sources is displayed. Raw image data can also be exported as multi-channel grayscale.")
public class ScreenShotMakerCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{

    @Parameter
    public BdvHandle bdvh;

    @Parameter(label="Screenshot Sampling [UNIT]", callback = "showNumPixels")
    public Double targetSamplingInXY = 1D;

    @Parameter(label="Show Raw Data")
    public boolean showRawData = false;

    private String pixelUnit = "Pixels";

    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvh );
        screenShotMaker.setPhysicalPixelSpacingInXY( targetSamplingInXY, pixelUnit );
        screenShotMaker.getRgbScreenShot().show();
        if( showRawData ) screenShotMaker.getRawScreenShot().show();
    }

    @Override
    public void initialize() {

        // adapt pixel unit
        //
        final MutableModuleItem< Double > pixelSizeItem = //
                getInfo().getMutableInput("targetSamplingInXY", Double.class);
        pixelUnit = bdvh.getViewerPanel().state().getCurrentSource().getSpimSource().getVoxelDimensions().unit();
        pixelSizeItem.setLabel( pixelSizeItem.getLabel().replace( "UNIT", pixelUnit ) );
    }

    // callback
    private void showNumPixels()
    {
        // TODO
    }
}
