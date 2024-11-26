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
package org.embl.mobie.command.context;

import bdv.util.BdvHandle;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Take Screenshot Stack")
public class ScreenShotStackMakerCommand extends ScreenShotMakerCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter(label="Slice distance (in above units)",
            persist = false,
            min = "0.0",
            style="format:#.00000",
            stepSize = "0.01")
    public Double sliceDistance = 1D;

    @Parameter(label="Number of slices above & below current",
            description = "For example, entering 5 here will result in:\n5 above + 1 current + 5 below = 11 slices in total.",
            persist = false)
    public Integer numSlices = 5;

    @Override
    public void run()
    {
        if ( MoBIE.getInstance().getSettings().values.isOpenedFromCLI() )
            MoBIE.imageJ.ui().showUI();

        // move viewer to numSlices below

        // collect all slices
        ImagePlus imageplus =  null;
        CompositeImage compositeImage = null;
        for ( int sliceIndex = 0; sliceIndex < numSlices; sliceIndex++ )
        {
            // adapt viewer transform


            ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, pixelUnit );
            screenShotMaker.run( targetSamplingInXY );

            if ( sliceIndex == 0 )
            {
                imageplus = screenShotMaker.getRGBImagePlus();
                compositeImage = screenShotMaker.getCompositeImagePlus();
            } else
            {
                // append RGB images
                imageplus.getStack().addSlice( screenShotMaker.getRGBImagePlus().getProcessor() );

                // append Float images
                ImageStack stack = screenShotMaker.getCompositeImagePlus().getStack();
                for ( int stackIndex = 0; stackIndex < stack.size(); stackIndex++ )
                {
                    compositeImage.getStack().addSlice( stack.getProcessor( stackIndex ) );
                }
            }
        }

        imageplus.show();
        compositeImage.show();
    }
}
