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
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.LUT;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
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

    public Roi[] getMasks()
    {
        return compositeImagePlus.getOverlay().toArray();
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
        final ArrayList< RandomAccessibleInterval< BitType > > maskCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures = new ArrayList<>();
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

        IJ.log( ThreadHelper.getNumIoThreads() + " threads working on blocks of " + Arrays.toString( blockSize ) );
        final long currentTimeMillis = System.currentTimeMillis();
        for ( SourceAndConverter< ?  > sac : sacs )
        {
            final RandomAccessibleInterval< FloatType > floatCapture
                    = ArrayImgs.floats( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            final RandomAccessibleInterval< BitType > maskCapture
                    = ArrayImgs.bits( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
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
                        RealRandomAccess< ? extends Type< ? > > sourceAccess = getRealRandomAccess( ( Source< Type< ? > > ) source, currentTimepoint, level, interpolate );
                        RandomAccessibleInterval< ? > sourceInterval = source.getSource( currentTimepoint, level );

                        // to collect raw data
                        final IntervalView< FloatType > floatCrop = Views.interval( floatCapture, interval );
                        final Cursor< FloatType > floatCursor = Views.iterable( floatCrop ).localizingCursor();
                        final RandomAccess< FloatType > floatAccess = floatCrop.randomAccess();

                        // to collect masks
                        final RandomAccess< BitType > maskAccess = Views.interval( maskCapture, interval ).randomAccess();

                        // to collect colored data
                        final RandomAccess< ARGBType > argbAccess = Views.interval( argbCapture, interval ).randomAccess();

                        final double[] canvasPosition = new double[ 3 ];
                        final double[] sourceRealPosition = new double[ 3 ];

                        final ARGBType argbType = new ARGBType();

                        // iterate through the target image in pixel units
                        while ( floatCursor.hasNext() )
                        {
                            // set the positions
                            floatCursor.fwd();
                            floatCursor.localize( canvasPosition );
                            floatAccess.setPosition( floatCursor );
                            maskAccess.setPosition( floatCursor );
                            argbAccess.setPosition( floatCursor );
                            targetCanvasToSourceTransform.apply( canvasPosition, sourceRealPosition );
                            sourceAccess.setPosition( sourceRealPosition );

                            // set the pixel values
                            if ( Intervals.contains( sourceInterval, new RealPoint( sourceRealPosition ) ) )
                            {
                                maskAccess.get().set( true );
                                setFloatPixelValue( sourceAccess, floatAccess );
                                setArgbPixelValue( converter, sourceAccess, argbAccess, argbType );
                            }
                            else
                            {
                                maskAccess.get().set( false );
                            }

                            // log progress
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

            floatCaptures.add( floatCapture );
            maskCaptures.add( maskCapture );
            argbCaptures.add( argbCapture );
            displayRanges.add( BdvHandleHelper.getDisplayRange( SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ) ) );
        }

        IJ.log( "Fetched data in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

        final double[] voxelSpacing = new double[ 3 ];
        Arrays.fill( voxelSpacing, targetVoxelSpacing );

        if ( ! floatCaptures.isEmpty() )
        {
            rgbImagePlus = createRGBImagePlus( voxelUnit, argbCaptures, voxelSpacing, sacs );
            compositeImagePlus = createCompositeImagePlus( voxelSpacing, voxelUnit, floatCaptures, maskCaptures, displayRanges );
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
            ArrayList< RandomAccessibleInterval< FloatType > > floatCaptures,
            ArrayList< RandomAccessibleInterval< BitType > > maskCaptures,
            ArrayList< double[] > displayRanges )
    {
        final ImagePlus imp = ImageJFunctions.wrap( Views.stack( floatCaptures ), "Floats" );
        final ImagePlus mask = ImageJFunctions.wrap( Views.stack( maskCaptures ), "Masks" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+floatCaptures.size()
                        +" slices=1 frames=1 unit=" + voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );

        final CompositeImage compositeImage = new CompositeImage( dup );

        Overlay rois = new Overlay();
        for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
        {
            final LUT lut = compositeImage.createLutFromColor( Color.WHITE );
            compositeImage.setC( channel );
            compositeImage.setChannelLut( lut );
            final double[] range = displayRanges.get( channel - 1 );
            compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
            mask.setPosition( channel );
            mask.getProcessor().setThreshold( 1.0, 255 );
            Roi roi = new ThresholdToSelection().convert( mask.getProcessor() );
            if ( roi == null )
                roi = new Roi( 0, 0, compositeImage.getWidth(), compositeImage.getHeight() );
            roi.setPosition( channel, 1, 1  );
            mask.getProcessor().setRoi( roi );
            rois.add( roi );
        }

        compositeImage.setOverlay( rois );
        compositeImage.setHideOverlay( true );
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
