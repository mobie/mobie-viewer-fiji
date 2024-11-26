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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Take Screenshot Stack")
public class ScreenShotStackMakerCommand extends ScreenShotMakerCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter(label="Slice distance (in above units)",
            persist = false,
            min = "0.0",
            style="format:#.00",
            stepSize = "0.01")
    public Double physicalSliceDistance = 1D;

    @Parameter(label="Number of slices above & below current",
            description = "For example, entering 5 here will result in:\n5 above + 1 current + 5 below = 11 slices in total.",
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
        double[] physicalA = new double[ 3 ];
        double[] physicalB = new double[ 3 ];
        double[] distance = new double[ 3 ];
        viewerTransform.apply( new double[]{ 0, 0, 0 }, physicalA );
        viewerTransform.apply( new double[]{ 0, 0, 1 }, physicalB );
        LinAlgHelpers.subtract( physicalA, physicalB, distance );
        System.out.println( Arrays.toString( distance ) );
        double screenToPhysicalScale = LinAlgHelpers.length( distance );
        System.out.println( screenToPhysicalScale );

        // move viewer to starting point
        viewerTransform.translate( 0, 0, - numSlices * physicalSliceDistance * screenToPhysicalScale );
        bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
        bdvHandle.getViewerPanel().requestRepaint();

        // collect all slices
        ImageStack rgbStack = new ImageStack();
        ImageStack compositeStack = new ImageStack();
        numSlices = numSlices * 2 + 1;
        Calibration calibration = null;

        for ( int sliceIndex = 0; sliceIndex < numSlices; sliceIndex++ )
        {
            // adapt viewer transform
            viewerTransform.translate( 0, 0, physicalSliceDistance * screenToPhysicalScale );
            bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
            bdvHandle.getViewerPanel().requestRepaint();

            IJ.log( "Slice index " + sliceIndex + "; screen centre: " + Arrays.toString( BdvHandleHelper.getWindowCentreInCalibratedUnits( bdvHandle ) ) );

            ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, pixelUnit );
            screenShotMaker.run( targetSamplingInXY );

            try
            {
                rgbStack.addSlice( screenShotMaker.getRGBImagePlus().getProcessor() );
                ImageStack stack = screenShotMaker.getCompositeImagePlus().getStack();
                for ( int i = 0; i < stack.size(); i++ )
                {
                    compositeStack.addSlice( stack.getProcessor( i + 1 ) );
                }

                calibration = screenShotMaker.getRGBImagePlus().getCalibration();
            }
            catch ( Exception e )
            {
                // FIXME
                // it could be that there were no SACs visible in that slice
                // which current causes an error, better would be to append an empty slice
                // the challenge is that if that is the first slice we don't know the size and datatype
                // of the ImageProcessor
                IJ.log( "[WARNING] Skipping empty screen shot slice" );
            }
        }

        bdvHandle.getViewerPanel().state().setViewerTransform( initialViewerTransform );
        bdvHandle.getViewerPanel().requestRepaint();

        calibration.pixelDepth = physicalSliceDistance;

        ImagePlus rgbImage = new ImagePlus( "RGB stack", rgbStack );
        rgbImage.setDimensions( 1, rgbStack.size(), 1 );
        rgbImage.setCalibration( calibration );
        rgbImage.show();

        CompositeImage compositeImage = new CompositeImage( new ImagePlus( "Composite stack", compositeStack ) );
        compositeImage.setDimensions( compositeStack.size() / rgbStack.size(), rgbStack.size(), 1 );
        compositeImage.setCalibration( calibration );
        compositeImage.show();
    }
}
