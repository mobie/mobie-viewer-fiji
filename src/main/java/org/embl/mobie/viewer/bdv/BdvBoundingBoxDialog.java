/*-
 * #%L
 * Fiji plugin for inspection and processing of big image data
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.util.Intervals;

import java.util.List;

public class BdvBoundingBoxDialog
{
    private BdvHandle bdvHandle;
    private final List< SourceAndConverter > sourceAndConverters;

    private FinalRealInterval initialInterval;
    private FinalRealInterval rangeInterval;
    private RealInterval interval;
    private int minTimepoint;
    private int maxTimepoint;
    private AffineTransform3D boxTransform;
    private TransformedRealBoxSelectionDialog.Result result;

    public BdvBoundingBoxDialog( BdvHandle bdvHandle, List< SourceAndConverter > sourceAndConverters )
    {
        this.bdvHandle = bdvHandle;
        this.sourceAndConverters = sourceAndConverters;

        // viewer transform: physical to screen (0,0,w,h)
        this.boxTransform = bdvHandle.getViewerPanel().state().getViewerTransform();

        // physical to screen with (-w/2,-h/2,w/2,h/2)
        boxTransform.translate( -0.5 * bdvHandle.getViewerPanel().getDisplay().getWidth(), -0.5 * bdvHandle.getViewerPanel().getDisplay().getHeight(), 0 );

        // remove bdv window scale from transform
        // note that the scale of the viewer transform is uniform in 3-D
        // thus we can just pick either width or height
        final double scale = 1.0 / bdvHandle.getViewerPanel().getDisplay().getWidth();
        final Scale3D scale3D = new Scale3D( scale, scale, scale );
        boxTransform.preConcatenate( scale3D );

        // inverse: box to physical
        boxTransform = boxTransform.inverse();

        // check that it is correct (just for debugging)
        final double[] center = { 0, 0, 0 };
        final double[] left = { -0.5, -0.5, -0.5 };
        final double[] right = { 0.5, 0.5, 0.5 };
        final double[] physicalLeft = { 0, 0, 0 };
        final double[] physicalRight = { 0, 0, 0 };
        final double[] physicalCenter = { 0, 0, 0 };
        boxTransform.apply( left, physicalLeft );
        boxTransform.apply( right, physicalRight );
        boxTransform.apply( center, physicalCenter );
    }

    public void showDialog()
    {
        setInitialSelectionAndRange();
        result = showRealBox( boxTransform );
    }

    public TransformedRealBoxSelectionDialog.Result getResult()
    {
        return result;
    }

    private TransformedRealBoxSelectionDialog.Result showRealBox( AffineTransform3D boxTransform )
    {
        return BdvFunctions.selectRealBox(
                bdvHandle,
                boxTransform,
                initialInterval,
                rangeInterval,
                BoxSelectionOptions.options()
                        .title( "Crop" )
                        .initialTimepointRange( bdvHandle.getViewerPanel().state().getCurrentTimepoint(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() )
                        .selectTimepointRange( 0, bdvHandle.getViewerPanel().state().getNumTimepoints() - 1  )
        );
    }

    private void setInitialSelectionAndRange( )
    {
        final FinalRealInterval viewerBoundingInterval = getViewerGlobalBoundingInterval( bdvHandle );

        double[] initialCenter = new double[ 3 ];
        double[] initialSize = new double[ 3 ];

        for (int d = 0; d < 3; d++)
        {
            initialCenter[ d ] = ( viewerBoundingInterval.realMax( d ) + viewerBoundingInterval.realMin( d ) ) / 2.0;
            initialSize[ d ] = ( viewerBoundingInterval.realMax( d ) - viewerBoundingInterval.realMin( d ) );
        }

        initialSize[ 2 ] = 10.0;

        double[] minInitial = new double[ 3 ];
        double[] maxInitial = new double[ 3 ];
        double[] minRange = new double[ 3 ];
        double[] maxRange = new double[ 3 ];

        for ( int d = 0; d < 3; d++ )
        {
            minInitial[  d ] = initialCenter[ d ] - initialSize[ d ] / 4;
            maxInitial[  d ] = initialCenter[ d ] + initialSize[ d ] / 4;
            minRange[  d ] = initialCenter[ d ] - initialSize[ d ] / 2;
            maxRange[  d ] = initialCenter[ d ] + initialSize[ d ] / 2;
        }

//        initialInterval = Intervals.createMinMax(
//                (long) minInitial[0], (long) minInitial[1], (long) minInitial[2],
//                (long) maxInitial[0], (long) maxInitial[1], (long) maxInitial[2]);

//        rangeInterval = Intervals.createMinMax(
//                (long) minRange[0], (long) minRange[1], (long) minRange[2],
//                (long) maxRange[0], (long) maxRange[1], (long) maxRange[2]);

        initialInterval = new FinalRealInterval(
                new double[]{-0.25,-0.25,-0.25},
                new double[]{+0.25,+0.25,+0.25});
        rangeInterval = new FinalRealInterval(
                new double[]{-0.5,-0.5,-0.5},
                new double[]{+0.5,+0.5,+0.5});
    }

    private static FinalRealInterval getViewerGlobalBoundingInterval( Bdv bdv )
    {
        AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( viewerTransform );
        viewerTransform = viewerTransform.inverse();
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];
        max[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
        max[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();
        final FinalRealInterval realInterval
                = viewerTransform.estimateBounds( new FinalInterval( min, max ) );
        return realInterval;
    }
}
