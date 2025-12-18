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

import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.embl.mobie.lib.bdv.ScreenShotStackMaker;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// create separate ImagePlus
@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Take Screenshot Stack")
public class ScreenShotStackMakerCommand extends ScreenShotMakerCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter(label="Slice distance (in above units)",
            persist = false,
            min = "0.0",
            style="format:#.000",
            stepSize = "0.001")
    public Double targetSamplingInZ = 1D;

    @Parameter(label="Number of slices (starting from current)",
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

        // collect data
        ScreenShotStackMaker maker = new ScreenShotStackMaker( bdvHandle, pixelUnit, numSlices );
        maker.run( targetSamplingInXY, targetSamplingInZ );
        maker.getOutputImps().forEach( o -> o.show() );
        bdvHandle.getViewerPanel().state().setViewerTransform( initialViewerTransform );
        bdvHandle.getViewerPanel().requestRepaint();
    }

    @Override
    public void initialize()
    {
        super.initialize();

        final MutableModuleItem< Double > targetSamplingItem =
                getInfo().getMutableInput("targetSamplingInZ", Double.class);
        targetSamplingItem.setValue( this, getTargetSampling() );
    }

    // callback
    @Override
    protected void showNumPixels()
    {
        final long[] sizeInPixels = ScreenShotMaker.getCaptureImageSizeInPixels( bdvHandle, targetSamplingInXY, numSlices );
        IJ.log( CAPTURE_SIZE_PIXELS + Arrays.toString( sizeInPixels ) );
        List< SourceAndConverter< ? > > sacs = MoBIEHelper.getVisibleImageSacs( bdvHandle );
        ArrayList< Type > types = MoBIEHelper.getTypes( sacs );

        // Compute total size
        long numPixels = 1;
        for ( long size : sizeInPixels )
            numPixels *= size;

        long totalSizeInBytes = 0;
        for ( Type type : types )
        {
            if ( type instanceof UnsignedByteType )
            {
                totalSizeInBytes += numPixels * 1;
            }
            else if ( type instanceof UnsignedShortType )
            {
                totalSizeInBytes += numPixels * 2;
            }
            else
            {
                totalSizeInBytes += numPixels * 4; // float
            }
        }

        // Compare to available memory
        long currentMemory = IJ.currentMemory();
        long maxMemory = IJ.maxMemory();
        long freeMem = maxMemory - currentMemory;
        double totalSizeInMB = totalSizeInBytes / (1024.0 * 1024.0);
        double freeSizeInMB = freeMem / (1024.0 * 1024.0);

        IJ.log( "Needed memory [GB]: " + String.format( "%.4f", totalSizeInMB / 1024 ) );
        IJ.log( "Free memory [GB]:" + String.format( "%.4f", freeSizeInMB / 1024 ) );

        if ( totalSizeInBytes > freeMem )
        {
            IJ.showMessage( "Not enough memory available!\n" +
                    "Increase the sampling or decrease the number of z-planes;\n" +
                    "or use a computer with more RAM.\n" +
                    "See IJ Log Window for details." );
        }
    }
}
