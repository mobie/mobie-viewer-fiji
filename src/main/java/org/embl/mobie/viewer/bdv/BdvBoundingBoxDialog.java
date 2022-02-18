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
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import java.util.List;

public class BdvBoundingBoxDialog
{
    private Bdv bdvHandle;
    private final List< SourceAndConverter > sourceAndConverters;

    private Interval initialInterval;
    private Interval rangeInterval;
    private RealInterval interval;
    private int minTimepoint;
    private int maxTimepoint;

    public BdvBoundingBoxDialog( BdvHandle bdvHandle, List< SourceAndConverter > sourceAndConverters)
    {
        this.bdvHandle = bdvHandle;
        this.sourceAndConverters = sourceAndConverters;
    }

    public void showRealBoxAndWaitForResult()
    {
        setInitialSelectionAndRange( );

        final TransformedRealBoxSelectionDialog.Result result = showRealBox( );

        if ( result.isValid() )
        {
            interval = result.getInterval();
            minTimepoint = result.getMinTimepoint();
            maxTimepoint = result.getMaxTimepoint();
        }
    }

    public RealInterval getInterval()
    {
        return interval;
    }

    public int getMinTimepoint()
    {
        return minTimepoint;
    }

    public int getMaxTimepoint()
    {
        return maxTimepoint;
    }

    private TransformedRealBoxSelectionDialog.Result showRealBox( )
    {
        final AffineTransform3D boxTransform = new AffineTransform3D();

        return BdvFunctions.selectRealBox(
                bdvHandle,
                boxTransform,
                initialInterval,
                rangeInterval,
                BoxSelectionOptions.options()
                        .title( "Crop" )
                        .initialTimepointRange( 0, 0 )
                        .selectTimepointRange( 0, 0 )
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

        initialInterval = Intervals.createMinMax(
                (long) minInitial[0], (long) minInitial[1], (long) minInitial[2],
                (long) maxInitial[0], (long) maxInitial[1], (long) maxInitial[2]);

        rangeInterval = Intervals.createMinMax(
                (long) minRange[0], (long) minRange[1], (long) minRange[2],
                (long) maxRange[0], (long) maxRange[1], (long) maxRange[2]);
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
