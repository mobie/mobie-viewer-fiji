/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import edu.mines.jtk.util.AtomicDouble;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.blend.AccumulateAlphaBlendingProjectorARGB;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.lib.source.AnnotatedLabelSource;
import org.embl.mobie.lib.source.AnnotationType;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.isSourceIntersectingCurrentView;

public class ScreenShotMaker
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private final BdvHandle bdvHandle;
    private String voxelUnit = "Pixels";
    private ImagePlus rgbImagePlus = null;
    private CompositeImage compositeImagePlus = null;
    private long[] screenshotDimensions = new long[2];
    private AffineTransform3D canvasToGlobalTransform;

    private Float outOfBoundsValue = null;

    public ScreenShotMaker( BdvHandle bdvHandle, String voxelUnit ) {
        this.bdvHandle = bdvHandle;
        this.voxelUnit = voxelUnit;
    }

    public ImagePlus getRGBImagePlus()
    {
        return rgbImagePlus;
    }

    public CompositeImage getCompositeImagePlus()
    {
        return compositeImagePlus;
    }

    public void setOutOfBoundsValue( Float outOfBoundsValue )
    {
        this.outOfBoundsValue = outOfBoundsValue;
    }

    public void run( Double targetSamplingInXY )
    {
        List< SourceAndConverter< ? > > sacs = getVisibleSourceAndConverters();
        run( sacs, targetSamplingInXY );
    }

    public void run( List< SourceAndConverter< ? > > sacs, double targetVoxelSpacing  )
    {
        if ( sacs.isEmpty() )
        {
            IJ.log( "No screen shot taken, as there were no images." );
            return;
        }

        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( viewerTransform );
        final int currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

        canvasToGlobalTransform = new AffineTransform3D();
        // target canvas to viewer canvas...
        double targetToViewer = targetVoxelSpacing / getViewerVoxelSpacing( bdvHandle );
        canvasToGlobalTransform.scale( targetToViewer, targetToViewer, 1.0 );
        // ...viewer canvas to global
        AffineTransform3D viewerToGlobal = viewerTransform.inverse();
        canvasToGlobalTransform.preConcatenate( viewerToGlobal );
        IJ.log( "Canvas to global transform: " + canvasToGlobalTransform );

        IJ.log( "Fetching data from " + sacs.size() + " images..."  );

        final ArrayList< RandomAccessibleInterval< FloatType > > floatCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< ARGBType > > argbSources = new ArrayList<>();
        final ArrayList< ARGBType > colors = new ArrayList<>();

        final ArrayList< double[] > displayRanges = new ArrayList<>();

        screenshotDimensions = getCaptureImageSizeInPixels( bdvHandle, targetVoxelSpacing );
        final long numPixels = screenshotDimensions[ 0 ] * screenshotDimensions[ 1 ];
        long pixelsPerThread = numPixels / ThreadHelper.getNumIoThreads();
        int dimensionsPerThread = (int) Math.sqrt( pixelsPerThread );
        int[] blockSize = { dimensionsPerThread, dimensionsPerThread };
        List< Interval > intervals = Grids.collectAllContainedIntervals(
                screenshotDimensions,
                blockSize );

        IJ.log( "Number of threads: " + ThreadHelper.getNumIoThreads() );
        IJ.log( "Block per thread: " + Arrays.toString( blockSize ) );
        final long currentTimeMillis = System.currentTimeMillis();
        for ( SourceAndConverter< ?  > sac : sacs )
        {
            final RandomAccessibleInterval< FloatType > rawCapture
                    = ArrayImgs.floats( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            final RandomAccessibleInterval< ARGBType > argbCapture
                    = ArrayImgs.argbs( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ]  );

            Source< ? > source = sac.getSpimSource();
            final Converter< ?, ? > converter = sac.getConverter();

            final int level = getLevel( source, targetVoxelSpacing );
            final AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( source, currentTimepoint, level );

            // global to source
            AffineTransform3D targetCanvasToSourceTransform = canvasToGlobalTransform.copy();
            AffineTransform3D globalToSource = sourceTransform.inverse();
            targetCanvasToSourceTransform.preConcatenate( globalToSource );

            boolean interpolate = ! ( source.getType() instanceof AnnotationType );

            final AtomicInteger pixelCount = new AtomicInteger();
            final AtomicDouble fractionDone = new AtomicDouble( 0.2 );
            ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
            for ( Interval interval : intervals )
            {
                futures.add
                (
                    ThreadHelper.ioExecutorService.submit( () ->
                    {
                        RealRandomAccess< ? extends Type< ? > > access = getRealRandomAccess( ( Source< Type< ? > > ) source, currentTimepoint, level, interpolate );
                        RandomAccessibleInterval< ? > sourceInterval = source.getSource( currentTimepoint, level );

                        // to collect raw data
                        final IntervalView< FloatType > floatCrop = Views.interval( rawCapture, interval );
                        final Cursor< FloatType > floatCaptureCursor = Views.iterable( floatCrop ).localizingCursor();
                        final RandomAccess< FloatType > floatCaptureAccess = floatCrop.randomAccess();

                        // to collect colored data
                        final IntervalView< ARGBType > argbCrop = Views.interval( argbCapture, interval );
                        final RandomAccess< ARGBType > argbCaptureAccess = argbCrop.randomAccess();

                        final double[] canvasPosition = new double[ 3 ];
                        final double[] sourceRealPosition = new double[ 3 ];

                        final ARGBType argbType = new ARGBType();

                        // iterate through the target image in pixel units
                        while ( floatCaptureCursor.hasNext() )
                        {
                            floatCaptureCursor.fwd();
                            floatCaptureCursor.localize( canvasPosition );
                            floatCaptureAccess.setPosition( floatCaptureCursor );
                            argbCaptureAccess.setPosition( floatCaptureCursor );

                            targetCanvasToSourceTransform.apply( canvasPosition, sourceRealPosition );
                            access.setPosition( sourceRealPosition );

                            boolean contains = Intervals.contains( sourceInterval, new RealPoint( sourceRealPosition ) );
                            if ( ! contains  &&  outOfBoundsValue != null )
                            {
                                floatCaptureAccess.get().setReal( outOfBoundsValue );
                                // TODO how to set the ARGB value?
                            }
                            else
                            {
                                setFloatPixelValue( access, floatCaptureAccess );
                                setArgbPixelValue( converter, access, argbCaptureAccess, argbType );
                            }

                            pixelCount.incrementAndGet();
                            final double currentFractionDone = 1.0 * pixelCount.get() / numPixels;
                            if ( currentFractionDone >= fractionDone.get() )
                            {
                                synchronized ( fractionDone )
                                {
                                    // check again, because meanwhile another thread might have
                                    // incremented fractionDone
                                    if ( currentFractionDone >= fractionDone.get() )
                                    {
                                        IJ.log(sac.getSpimSource().getName() + ": " + ( Math.round( 100 * fractionDone.get() ) + "%" ) );
                                        fractionDone.addAndGet( 0.2 );
                                    }
                                }
                            }
                        }
                    } )
                );
            }

            ThreadHelper.waitUntilFinished( futures );

            floatCaptures.add( rawCapture );
            argbSources.add( argbCapture );
            displayRanges.add( BdvHandleHelper.getDisplayRange( SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ) ) );
        }

        IJ.log( "Fetched data in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

        final double[] voxelSpacing = new double[ 3 ];
        Arrays.fill( voxelSpacing, targetVoxelSpacing );

        if ( ! floatCaptures.isEmpty() )
        {
            rgbImagePlus = createRGBImagePlus( voxelUnit, argbSources, voxelSpacing, sacs );
            compositeImagePlus = createCompositeImagePlus( voxelSpacing, voxelUnit, floatCaptures, colors, displayRanges );
        }
    }

    public AffineTransform3D getCanvasToGlobalTransform()
    {
        return canvasToGlobalTransform;
    }

    private List< SourceAndConverter< ? > > getVisibleSourceAndConverters()
    {
        final List< SourceAndConverter <?> > visibleSacs = MoBIEHelper.getVisibleSacs( bdvHandle );

        List< SourceAndConverter< ? > > sacs = new ArrayList<>();
        for ( SourceAndConverter< ?  > sac : visibleSacs )
        {
            // TODO: can we determine from BDV whether a source is intersecting viewer plane?
            //       why do we need is2D=false ?
            if ( ! isSourceIntersectingCurrentView( bdvHandle, sac.getSpimSource(), false ) )
                continue;
            sacs.add( sac );
        }
        return sacs;
    }

    private void setArgbPixelValue( Converter converter, RealRandomAccess< ? > access, RandomAccess< ARGBType > argbCaptureAccess, ARGBType argbType )
    {
        final Object pixelValue = access.get();

        if ( pixelValue instanceof ARGBType )
            argbType.set( ( ARGBType ) pixelValue );
        else
            converter.convert( pixelValue, argbType );

        argbCaptureAccess.get().set( argbType.get() );
    }

    private void setFloatPixelValue( RealRandomAccess< ? extends Type< ? > > access, RandomAccess< FloatType > floatCaptureAccess )
    {
        final Type< ? > type = access.get();
        if ( type instanceof RealType )
        {
            floatCaptureAccess.get().setReal( ( ( RealType ) type ).getRealDouble() );
        }
        else if ( type instanceof AnnotationType )
        {
            try
            {
                final Annotation annotation = ( Annotation ) ( ( AnnotationType< ? > ) type ).getAnnotation();
                if ( annotation != null )
                    floatCaptureAccess.get().setReal( annotation.label() );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            throw new UnsupportedOperationException( "Cannot render " + type.getClass() );
        }
    }

    private RealRandomAccess< ? extends Type< ? > > getRealRandomAccess( Source< Type< ? > > source, int t, int level, boolean interpolate )
    {
        if ( interpolate )
        {
            Interpolation interpolation = bdvHandle.getViewerPanel().state().getInterpolation();
            return source.getInterpolatedSource( t, level, interpolation ).realRandomAccess();
        }
        else
        {
            // e.g., for label masks we do not want to interpolate
            return source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ).realRandomAccess();
        }
    }

    private ImagePlus createRGBImagePlus(
            String physicalUnit,
            ArrayList< RandomAccessibleInterval< ARGBType > > argbSources,
            double[] voxelSpacing,
            List< SourceAndConverter< ? > > sacs )
    {
        final RandomAccessibleInterval< ARGBType > argbTarget = ArrayImgs.argbs( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ]  );
        createARGBprojection( argbSources, argbTarget, sacs );
        return asImagePlus( argbTarget, physicalUnit, voxelSpacing );
    }

    private void createARGBprojection( ArrayList< RandomAccessibleInterval< ARGBType > > argbSources, RandomAccessibleInterval< ARGBType > argbTarget, List< SourceAndConverter< ? > > sacs )
    {
        final Cursor< ARGBType > argbCursor = Views.iterable( argbTarget ).localizingCursor();
        final int numVisibleSources = argbSources.size();
        Cursor< ARGBType >[] cursors = getCursors( argbSources, numVisibleSources );
        final boolean[] occlusions = AccumulateAlphaBlendingProjectorARGB.getAlphaBlending( sacs );
        final int[] order = AccumulateAlphaBlendingProjectorARGB.getOrder( sacs );

        while ( argbCursor.hasNext() )
        {
            try
            {
                argbCursor.fwd();
                for ( int i = 0; i < numVisibleSources; i++ )
                    cursors[ i ].fwd();
                final int argbIndex = AccumulateAlphaBlendingProjectorARGB.getArgbIndex( cursors, occlusions, order );
                argbCursor.get().set( argbIndex );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    public static long[] getCaptureImageSizeInPixels( BdvHandle bdvHandle, double samplingXY )
    {
        final double viewerVoxelSpacing = getViewerVoxelSpacing( bdvHandle );

        final double[] bdvWindowPhysicalSize = getBdvWindowPhysicalSize( bdvHandle, viewerVoxelSpacing );

        final long[] capturePixelSize = new long[ 2 ];
        for ( int d = 0; d < 2; d++ )
        {
            capturePixelSize[ d ] = ( long ) ( Math.ceil( bdvWindowPhysicalSize[ d ] / samplingXY ) );
        }

        return capturePixelSize;
    }

    private static double[] getBdvWindowPhysicalSize( BdvHandle bdvHandle, double viewerVoxelSpacing )
    {
        final double[] bdvWindowPhysicalSize = new double[ 2 ];
        final int w = bdvHandle.getViewerPanel().getWidth();
        final int h = bdvHandle.getViewerPanel().getHeight();
        bdvWindowPhysicalSize[ 0 ] = w * viewerVoxelSpacing;
        bdvWindowPhysicalSize[ 1 ] = h * viewerVoxelSpacing;
        return bdvWindowPhysicalSize;
    }

    private static Cursor< ARGBType >[] getCursors( ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures, int numVisibleSources )
    {
        Cursor[] cursors = new Cursor[ numVisibleSources ];
        for ( int i = 0; i < numVisibleSources; i++ )
            cursors[ i ] = Views.iterable( argbCaptures.get( i ) ).cursor();
        return cursors;
    }

    private ImagePlus asImagePlus( RandomAccessibleInterval< ARGBType > argbCapture, String physicalUnit, double[] voxelSpacing )
    {
        final ImagePlus rgbImage = ImageJFunctions.wrap( argbCapture, "RGB" );

        IJ.run( rgbImage,
                "Properties...",
                "channels=" + 1
                        +" slices=1 frames=1 unit=" + physicalUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );
        return rgbImage;
    }

    public static CompositeImage createCompositeImagePlus(
            double[] voxelSpacing,
            String voxelUnit,
            ArrayList< RandomAccessibleInterval< FloatType > > rais,
            ArrayList< ARGBType > colors,
            ArrayList< double[] > displayRanges )
    {
        final RandomAccessibleInterval< FloatType > stack = Views.stack( rais );

        final ImagePlus imp = ImageJFunctions.wrap( stack, "Multi-Channel" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+rais.size()
                        +" slices=1 frames=1 unit=" + voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );

        final CompositeImage compositeImage = new CompositeImage( dup );

        for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
        {
            final LUT lut = compositeImage.createLutFromColor( Color.WHITE );
            compositeImage.setC( channel );
            compositeImage.setChannelLut( lut );
            final double[] range = displayRanges.get( channel - 1 );
            compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
        }

        compositeImage.setTitle( "Multi-Channel" );
        return compositeImage;
    }

    private RealRandomAccess< ? extends RealType< ? > >
    getRealTypeRealRandomAccess( Source< ? > source, int t, int level, boolean interpolate )
    {
        if ( source instanceof AnnotatedLabelSource )
        {
            int a = 1;
        }

        if ( interpolate )
            return ( RealRandomAccess<? extends RealType<?>> ) source.getInterpolatedSource( t, level, Interpolation.NLINEAR).realRandomAccess();
        else
            return ( RealRandomAccess<? extends RealType<?>> ) source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR).realRandomAccess();
    }
}
