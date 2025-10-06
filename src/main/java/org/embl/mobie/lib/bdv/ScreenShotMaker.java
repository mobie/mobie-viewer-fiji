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
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.util.Corners;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.blend.MoBIEAccumulateProjectorARGB;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.lib.source.label.AnnotatedLabelSource;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.SourceHelper;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;

public class ScreenShotMaker
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private final BdvHandle bdvHandle;
    private String voxelUnit = "Pixels";
    private ImagePlus rgbImagePlus = null;
    private CompositeImage compositeImagePlus = null;
    private long[] screenshotDimensions = new long[2];
    private AffineTransform3D canvasToGlobalTransform;
    private AffineTransform3D viewerToGlobal;
    private Corners corners;
    private String progress;

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
        List< SourceAndConverter< ? > > sacs = MoBIEHelper.getVisibleSacsInCurrentView( bdvHandle );

        run( sacs, targetSamplingInXY );
    }

    public void run( List< SourceAndConverter< ? > > sacs, double targetVoxelSpacing  )
    {
        if ( sacs.isEmpty() )
        {
            IJ.log( "No screen shot taken, as there were no images." );
            return;
        }

        corners = MoBIEHelper.getBdvWindowGlobalCorners( bdvHandle );

        final int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
        final AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform( );
        canvasToGlobalTransform = new AffineTransform3D();
        // target canvas to viewer canvas...
        double targetToViewer = targetVoxelSpacing / getViewerVoxelSpacing( bdvHandle );
        canvasToGlobalTransform.scale( targetToViewer, targetToViewer, 1.0 );
        // ...viewer canvas to global
        viewerToGlobal = viewerTransform.inverse();
        canvasToGlobalTransform.preConcatenate( viewerToGlobal );
        //IJ.log( "Canvas to global transform: " + canvasToGlobalTransform );

        //IJ.log( "Fetching data from " + sacs.size() + " image(s)..."  );

        final ArrayList< RandomAccessibleInterval< ? extends RealType< ? >  > > realCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< BitType > > maskCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures = new ArrayList<>();

        final ArrayList< double[] > displayRanges = new ArrayList<>();

        screenshotDimensions = getCaptureImageSizeInPixels( bdvHandle, targetVoxelSpacing );
        final long numPixels = screenshotDimensions[ 0 ] * screenshotDimensions[ 1 ];
        long pixelsPerThread = numPixels / ThreadHelper.getNumIoThreads();
        int dimensionsPerThread = (int) Math.sqrt( pixelsPerThread );
        int[] blockSize = { dimensionsPerThread, dimensionsPerThread };
        List< Interval > intervals = Grids.collectAllContainedIntervals(
                screenshotDimensions,
                blockSize );

        // IJ.log( ThreadHelper.getNumIoThreads() + " threads working on blocks of " + Arrays.toString( blockSize ) );
        final long currentTimeMillis = System.currentTimeMillis();


        ArrayList< Type > types = new ArrayList<>();
        for ( SourceAndConverter< ? > sac : sacs )
        {
            Image< ? > image = DataStore.sourceToImage().get( sac );
            if ( image instanceof RegionAnnotationImage )
                continue;

            if ( image instanceof AnnotatedLabelImage )
            {
                RandomAccessibleInterval< ? extends IntegerType< ? > > source = ( ( AnnotatedLabelImage< ? > ) image ).getLabelImage().getSourcePair().getSource().getSource( 0, 0 );
                types.add( Util.getTypeFromInterval( source ) );
            }
            else
            {
                types.add( ( Type ) Util.getTypeFromInterval( sac.getSpimSource().getSource( 0, 0 ) ) );
            }
        }

        List< SourceAndConverter< ? > > dataSacs = sacs.stream()
                .filter( sac -> !( DataStore.sourceToImage().get( sac ) instanceof RegionAnnotationImage ) )
                .collect( Collectors.toList() );

        boolean allByte = types.stream()
                .allMatch( t -> t instanceof UnsignedByteType );

        boolean allByteOrShort = types.stream()
                .allMatch( t -> ( t instanceof UnsignedShortType ) || ( t instanceof UnsignedByteType ) );

        for ( SourceAndConverter< ?  > sac : sacs )
        {
            RandomAccessibleInterval< ? extends RealType< ? > > realRAI;

            if ( allByte )
            {
                // ImageJ 8-bit
                realRAI = ArrayImgs.unsignedBytes( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            }
            else if ( allByteOrShort )
            {
                // ImageJ 16-bit
                realRAI = ArrayImgs.unsignedShorts( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            }
            else
            {
                // ImageJ 32-bit
                realRAI = ArrayImgs.floats( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            }

            final RandomAccessibleInterval< BitType > maskRAI
                    = ArrayImgs.bits( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ] );
            final RandomAccessibleInterval< ARGBType > argbRAI
                    = ArrayImgs.argbs( screenshotDimensions[ 0 ], screenshotDimensions[ 1 ]  );

            Source< ? > source = sac.getSpimSource();
            final Converter< ?, ? > converter = sac.getConverter();
            double[] displayRange = BdvHandleHelper.getDisplayRange( SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ) );
            final int level = getLevel( source, targetVoxelSpacing );
            final AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( source, timePoint, level );

            // global to source
            AffineTransform3D targetCanvasToSourceTransform = canvasToGlobalTransform.copy();
            AffineTransform3D globalToSource = sourceTransform.inverse();
            targetCanvasToSourceTransform.preConcatenate( globalToSource );

            boolean interpolate = ! ( source.getType() instanceof AnnotationType );

            final AtomicInteger pixelCount = new AtomicInteger();
            final AtomicDouble fractionDone = new AtomicDouble( 0.2 );
            ArrayList< Future< ? > > futures = ThreadHelper.getFutures();

            progress = "Capturing " + sac.getSpimSource().getName() + ": 0%";
            IJ.log( progress );
            for ( Interval interval : intervals )
            {
                futures.add
                (
                    ThreadHelper.ioExecutorService.submit( () ->
                    {
                        RealRandomAccess< ? extends Type< ? > > sourceAccess = getRealRandomAccess( ( Source< Type< ? > > ) source, timePoint, level, interpolate );
                        WritableBox sourceMask = SourceHelper.estimateDataMask( source, timePoint, level, true );
                        //RandomAccessibleInterval< ? > sourceInterval = source.getSource( currentTimepoint, level );

                        // to collect raw data
                        final IntervalView< ? extends RealType< ? >  > realCrop = Views.interval( realRAI, interval );
                        final Cursor< ? extends RealType< ? >  > targetCursor = Views.iterable( realCrop ).localizingCursor();
                        final RandomAccess< ? extends RealType< ? >  > targetAccess = realCrop.randomAccess();

                        // to collect masks
                        final RandomAccess< BitType > maskAccess = Views.interval( maskRAI, interval ).randomAccess();

                        // to collect colored data
                        final RandomAccess< ARGBType > argbAccess = Views.interval( argbRAI, interval ).randomAccess();

                        final double[] canvasPosition = new double[ 3 ];
                        final double[] sourceRealPosition = new double[ 3 ];

                        final ARGBType argbType = new ARGBType();

                        // iterate through the target image in pixel units
                        while ( targetCursor.hasNext() )
                        {
                            // set the positions
                            targetCursor.fwd();
                            targetCursor.localize( canvasPosition );
                            targetAccess.setPosition( targetCursor );
                            maskAccess.setPosition( targetCursor );
                            argbAccess.setPosition( targetCursor );
                            targetCanvasToSourceTransform.apply( canvasPosition, sourceRealPosition );
                            sourceAccess.setPosition( sourceRealPosition );

                            // set the pixel and mask values depending on whether the
                            // pixel is within the source data
                            if ( sourceMask.test( new RealPoint( sourceRealPosition ) ) )
                            {
                                maskAccess.get().set( true );
                                setArgbPixelValue( converter, sourceAccess, argbAccess, argbType );

                                if ( dataSacs.contains( sac ) )
                                    setPixelValue( sourceAccess, targetAccess );
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
                                        progress += ", " + Math.round( 100 * fractionDone.get() ) + "%";
                                        IJ.log( "\\Update:" + progress );
                                        fractionDone.addAndGet( 0.2 );
                                    }
                                }
                            }
                        }
                    } )
                );
            }

            ThreadHelper.waitUntilFinished( futures );

            if ( dataSacs.contains( sac ) )
            {
                realCaptures.add( realRAI );
                maskCaptures.add( maskRAI );
            }
            argbCaptures.add( argbRAI );
            displayRanges.add( displayRange );
        }

        //IJ.log( "Fetched data in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

        final double[] voxelSpacing = new double[ 3 ];
        Arrays.fill( voxelSpacing, targetVoxelSpacing );

        if ( ! realCaptures.isEmpty() )
        {
            rgbImagePlus = createRGBImagePlus( voxelUnit, argbCaptures, voxelSpacing, sacs );

            // TODO: instead of a composite image we could return multiple images here, one per sac,
            //  this would also help with the datatype
            //  one has to think about the pros and cons of having them in one image...
            compositeImagePlus = createCompositeImagePlus(
                    voxelSpacing,
                    voxelUnit,
                    realCaptures,
                    maskCaptures,
                    displayRanges );
        }
    }

    public Corners getCorners()
    {
        return corners;
    }

    public AffineTransform3D getCanvasToGlobalTransform()
    {
        return canvasToGlobalTransform;
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

    private void setPixelValue(
            RealRandomAccess< ? extends Type< ? > > sourceAccess,
            RandomAccess< ? extends RealType< ? > > targetAccess )
    {
        final Type< ? > type = sourceAccess.get();
        if ( type instanceof RealType )
        {
            double realDouble = ( ( RealType ) type ).getRealDouble();
            targetAccess.get().setReal( realDouble );
        }
        else if ( type instanceof AnnotationType )
        {
            try
            {
                final Annotation annotation = ( Annotation ) ( ( AnnotationType< ? > ) type ).getAnnotation();
                if ( annotation != null )
                    targetAccess.get().setReal( annotation.label() );
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
        final boolean[] alphaBlending = MoBIEAccumulateProjectorARGB.getAlphaBlending( sacs );
        final boolean[] andBlending = MoBIEAccumulateProjectorARGB.getAndBlending( sacs );

        final int[] order = MoBIEAccumulateProjectorARGB.getOrder( sacs );

        while ( argbCursor.hasNext() )
        {
            try
            {
                argbCursor.fwd();
                for ( int i = 0; i < numVisibleSources; i++ )
                    cursors[ i ].fwd();
                final int argbIndex = MoBIEAccumulateProjectorARGB.getArgbIndex( cursors, alphaBlending, andBlending, order );
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
            ArrayList< RandomAccessibleInterval< ? extends RealType< ? >  > > realRAIs,
            ArrayList< RandomAccessibleInterval< BitType > > maskRAIs,
            ArrayList< double[] > displayRanges )
    {

        final ImagePlus imp = ImageJFunctions.wrap( Views.stack( (ArrayList) realRAIs ), "Floats" );
        final ImagePlus mask = ImageJFunctions.wrap( Views.stack( maskRAIs ), "Masks" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+realRAIs.size()
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
