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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.embl.mobie.lib.bdv.ScreenShotStackMaker;
import org.embl.mobie.lib.util.Corners;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.*;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Take Screenshot Stack (Devel)")
public class ScreenShotStackMaker2Command extends ScreenShotMakerCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter(label="Slice distance (in above units)",
            persist = false,
            min = "0.0",
            style="format:#.000",
            stepSize = "0.001")
    public Double targetSamplingInZ = 1D;

    @Parameter(label="Number of slices",
            description = "This is above and including the current slice.",
            persist = false)
    public Integer numSlices = 5;

    @Override
    public void run()
    {
        if ( MoBIE.getInstance().getSettings().values.isOpenedFromCLI() )
            MoBIE.imageJ.ui().showUI();

        AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
        AffineTransform3D initialViewerTransform = viewerTransform.copy();

        // compute scaling from viewer to physical coordinates along the current viewing axis
//        double[] physicalA = new double[ 3 ];
//        double[] physicalB = new double[ 3 ];
//        double[] distance = new double[ 3 ];
//        viewerTransform.apply( new double[]{ 0, 0, 0 }, physicalA );
//        viewerTransform.apply( new double[]{ 0, 0, 1 }, physicalB );
//        LinAlgHelpers.subtract( physicalA, physicalB, distance );
//        System.out.println( Arrays.toString( distance ) );
//        double screenToPhysicalScale = LinAlgHelpers.length( distance );
//        System.out.println( screenToPhysicalScale );
//
//        // move viewer to starting point
//        viewerTransform.translate( 0, 0, -numSlices * targetSamplingInZ * screenToPhysicalScale );
//        bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
//        bdvHandle.getViewerPanel().requestRepaint();

        // collect data
        ScreenShotStackMaker maker = new ScreenShotStackMaker( bdvHandle, pixelUnit, numSlices );
        maker.run( targetSamplingInXY, targetSamplingInZ );
        List< ImagePlus > outputImps = maker.getOutputImps();
        for ( ImagePlus outputImp : outputImps )
        {
            outputImp.show();
        }

        bdvHandle.getViewerPanel().state().setViewerTransform( initialViewerTransform );
        bdvHandle.getViewerPanel().requestRepaint();



//        IJ.log( "Lowest plane corners:" );
//        IJ.log( corners.get( 0 ).toString() );
//        IJ.log( "Highest plane corners:" );
//        IJ.log( corners.get( 1 ).toString() );
    }

    @Override
    public void initialize()
    {
        super.initialize();

        final MutableModuleItem< Double > targetSamplingItem =
                getInfo().getMutableInput("targetSamplingInZ", Double.class);
        targetSamplingItem.setValue( this, getTargetSampling() );
    }
}
