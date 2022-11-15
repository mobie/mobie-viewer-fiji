/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
import net.imglib2.type.Type;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.bdv.render.AccumulateAlphaBlendingProjectorARGB;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.source.AnnotatedLabelSource;
import org.embl.mobie.viewer.source.AnnotationType;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.isSourceIntersectingCurrentView;

/**
 * BigDataViewer Playground Action --
 * ScreenShotMaker
 * Author: @haesleinhuepf, @tischi
 *         December 2019
 */

public class ScreenShotMaker
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private final BdvHandle bdvHandle;
    private final ISourceAndConverterService sacService;
    private double samplingXY = 1;
    private String physicalUnit = "Pixels";
    private boolean sourceInteractionWithViewerPlaneOnly2D = false; // TODO: maybe remove in the future
    ImagePlus rgbImagePlus = null;
    private CompositeImage compositeImagePlus = null;
    private long[] captureImageSizeInPixels = new long[2];

    public ScreenShotMaker( BdvHandle bdvHandle) {
        this.bdvHandle = bdvHandle;
        this.sacService = SourceAndConverterServices.getSourceAndConverterService();
    }

    public void setPhysicalPixelSpacingInXY(double spacing, String unit) {
        this.rgbImagePlus = null;
        this.samplingXY = spacing;
        this.physicalUnit = unit;
    }

    public void setSourceInteractionWithViewerPlaneOnly2D(boolean sourceInteractionWithViewerPlaneOnly2D) {
        this.rgbImagePlus = null;
        this.sourceInteractionWithViewerPlaneOnly2D = sourceInteractionWithViewerPlaneOnly2D;
    }

    private void process() {
        if ( rgbImagePlus != null)
            return;
        createScreenShot();
    }

    public ImagePlus getRgbScreenShot() {
        process();
        return rgbImagePlus;
    }

    public CompositeImage getRawScreenShot()
    {
        process();
        return compositeImagePlus;
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

    private void createScreenShot()
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( viewerTransform );

        captureImageSizeInPixels = getCaptureImageSizeInPixels( bdvHandle, samplingXY );

        final ArrayList< RandomAccessibleInterval< UnsignedShortType > > rawCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< ARGBType > > argbSources = new ArrayList<>();
        final ArrayList< ARGBType > colors = new ArrayList<>();

        final ArrayList< double[] > displayRanges = new ArrayList<>();

        final List< SourceAndConverter <?> > visibleSacs = getVisibleSacs( bdvHandle );
        if ( visibleSacs.size() == 0 ) return;

        List< SourceAndConverter< ? > > sacs = new ArrayList<>();
        for ( SourceAndConverter< ?  > sac : visibleSacs )
        {
            if ( ! isSourceIntersectingCurrentView( bdvHandle, sac.getSpimSource(), sourceInteractionWithViewerPlaneOnly2D ) )
                continue;
            sacs.add( sac );
        }
        if ( sacs.size() == 0 ) return;

        final int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

        for ( SourceAndConverter< ?  > sac : sacs )
        {
            final RandomAccessibleInterval< UnsignedShortType > rawCapture
                    = ArrayImgs.unsignedShorts( captureImageSizeInPixels[ 0 ], captureImageSizeInPixels[ 1 ] );
            final RandomAccessibleInterval< ARGBType > argbCapture
                    = ArrayImgs.argbs( captureImageSizeInPixels[ 0 ], captureImageSizeInPixels[ 1 ]  );

            Source< ? > source = sac.getSpimSource();
            final Converter converter = sac.getConverter();

            final int level = getLevel( source, samplingXY );
            final AffineTransform3D sourceTransform =
                    BdvHandleHelper.getSourceTransform( source, t, level );

            AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
            viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
            viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

            final double canvasStepSize = samplingXY / getViewerVoxelSpacing( bdvHandle );

            boolean interpolate = ! ( source.getType() instanceof AnnotationType );

            Grids.collectAllContainedIntervals(
                    Intervals.dimensionsAsLongArray( argbCapture ),
                    new int[]{100, 100}).parallelStream().forEach( interval ->
            {
                RealRandomAccess< ? extends Type< ? > > access = getRealRandomAccess( ( Source< Type< ? > > ) source, t, level, interpolate );

                // to collect raw data
                final IntervalView< UnsignedShortType > rawCrop = Views.interval( rawCapture, interval );
                final Cursor< UnsignedShortType > rawCaptureCursor = Views.iterable( rawCrop ).localizingCursor();
                final RandomAccess< UnsignedShortType > rawCaptureAccess = rawCrop.randomAccess();

                // to collect colored data
                final IntervalView< ARGBType > argbCrop = Views.interval( argbCapture, interval );
                final RandomAccess< ARGBType > argbCaptureAccess = argbCrop.randomAccess();

                final double[] canvasPosition = new double[ 3 ];
                final double[] sourceRealPosition = new double[ 3 ];

                final ARGBType argbType = new ARGBType();

                // iterate through the target image in pixel units
                while ( rawCaptureCursor.hasNext() )
                {
                    rawCaptureCursor.fwd();
                    rawCaptureCursor.localize( canvasPosition );
                    rawCaptureAccess.setPosition( rawCaptureCursor );
                    argbCaptureAccess.setPosition( rawCaptureCursor );

                    // canvasPosition is in calibrated units
                    // canvasStepSize is the step size that is needed to get
                    // the desired resolution in the output image
                    canvasPosition[ 0 ] *= canvasStepSize;
                    canvasPosition[ 1 ] *= canvasStepSize;

                    viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );
                    access.setPosition( sourceRealPosition );
                    setUnsignedShortCapturePixelValue( access, rawCaptureAccess );
                    setArgbCapturePixelValue( converter, access, argbCaptureAccess, argbType );
                }
            });

            rawCaptures.add( rawCapture );
            argbSources.add( argbCapture );
            // colors.add( getSourceColor( bdv, sourceIndex ) ); Not used, show GrayScale
            displayRanges.add( BdvHandleHelper.getDisplayRange( sacService.getConverterSetup( sac ) ) );
        }

        final double[] voxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 2; d++ )
            voxelSpacing[ d ] = samplingXY;

        voxelSpacing[ 2 ] = getViewerVoxelSpacing( bdvHandle ); // TODO: What to put here?

        if ( rawCaptures.size() > 0 )
        {
            rgbImagePlus = createImagePlus( physicalUnit, argbSources, voxelSpacing, sacs );
            compositeImagePlus = createCompositeImage( voxelSpacing, physicalUnit, rawCaptures, colors, displayRanges );
        }
    }

    private List< SourceAndConverter<?> > getVisibleSacs( BdvHandle bdv )
    {
        final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getBdvDisplayService();

        final List< SourceAndConverter<?> > sacs = displayService.getSourceAndConverterOf( bdvHandle );
        List< SourceAndConverter<?> > visibleSacs = new ArrayList<>(  );
        for ( SourceAndConverter sac : sacs )
        {
            // TODO: this does not evaluate to true for all visible sources
            if ( displayService.isVisible( sac, bdv ) )
            {
                if (sac.getSpimSource().getSource(0,0)!=null) // TODO improve this hack that allows to discard overlays source from screenshot
                    visibleSacs.add( sac );
            }
        }

        return visibleSacs;
    }

    private void setArgbCapturePixelValue( Converter converter, RealRandomAccess< ? > access, RandomAccess< ARGBType > argbCaptureAccess, ARGBType argbType )
    {
        final Object pixelValue = access.get();

        if ( pixelValue instanceof ARGBType )
            argbType.set( ( ARGBType ) pixelValue );
        else
            converter.convert( pixelValue, argbType );

        argbCaptureAccess.get().set( argbType.get() );
    }

    private void setUnsignedShortCapturePixelValue( RealRandomAccess< ? extends Type< ? > > access, RandomAccess< UnsignedShortType > realCaptureAccess )
    {
        final Type< ? > type = access.get();
        if ( type instanceof RealType )
        {
            realCaptureAccess.get().setReal( ( ( RealType ) type ).getRealDouble() );
        }
        else if ( type instanceof AnnotationType )
        {
            try
            {
                final Annotation annotation = ( Annotation ) ( ( AnnotationType< ? > ) type ).getAnnotation();
                if ( annotation != null )
                    realCaptureAccess.get().setReal( annotation.label() );
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
           return source.getInterpolatedSource( t, level, Interpolation.NLINEAR ).realRandomAccess();
        else
            return source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ).realRandomAccess();
    }

    private ImagePlus createImagePlus(
            String physicalUnit,
            ArrayList< RandomAccessibleInterval< ARGBType > > argbSources,
            double[] voxelSpacing,
            List< SourceAndConverter< ? > > sacs )
    {
        final RandomAccessibleInterval< ARGBType > argbTarget = ArrayImgs.argbs( captureImageSizeInPixels[ 0 ], captureImageSizeInPixels[ 1 ]  );

        project( argbSources, argbTarget, sacs );

        return asImagePlus( argbTarget, physicalUnit, voxelSpacing );
    }

    private void project( ArrayList< RandomAccessibleInterval< ARGBType > > argbSources, RandomAccessibleInterval< ARGBType > argbTarget, List< SourceAndConverter< ? > > sacs )
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

//    private void projectUsingSumProjector( ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures, RandomAccessibleInterval< ARGBType > argbCapture )
//    {
//        final Cursor< ARGBType > argbCursor = Views.iterable( argbCapture ).localizingCursor();
//        final int numVisibleSources = argbCaptures.size();
//
//        Cursor< ARGBType >[] cursors = getCursors( argbCaptures, numVisibleSources );
//
//        while ( argbCursor.hasNext() )
//        {
//            argbCursor.fwd();
//            for ( int i = 0; i < numVisibleSources; i++ )
//                cursors[ i ].fwd();
//
//            final int argbIndex = AccumulateSumProjectorARGB.getArgbIndex( cursors );
//            argbCursor.get().set( argbIndex );
//        }
//    }
//
//    private void projectUsingAverageProjector( ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures, RandomAccessibleInterval< ARGBType > argbCapture )
//    {
//        final Cursor< ARGBType > argbCursor = Views.iterable( argbCapture ).localizingCursor();
//        final int numVisibleSources = argbCaptures.size();
//
//        Cursor< ARGBType >[] cursors = getCursors( argbCaptures, numVisibleSources );
//
//        while ( argbCursor.hasNext() )
//        {
//            argbCursor.fwd();
//            for ( int i = 0; i < numVisibleSources; i++ )
//                cursors[ i ].fwd();
//
//            final int argbIndex = AccumulateAverageProjectorARGB.getArgbIndex( cursors );
//            argbCursor.get().set( argbIndex );
//        }
//    }
//
    private Cursor< ARGBType >[] getCursors( ArrayList< RandomAccessibleInterval< ARGBType > > argbCaptures, int numVisibleSources )
    {
        Cursor< ARGBType >[] cursors = new Cursor[ numVisibleSources ];
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

    public static CompositeImage createCompositeImage(
            double[] voxelSpacing,
            String voxelUnit,
            ArrayList< RandomAccessibleInterval< UnsignedShortType > > rais,
            ArrayList< ARGBType > colors,
            ArrayList< double[] > displayRanges )
    {
        final RandomAccessibleInterval< UnsignedShortType > stack = Views.stack( rais );

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
            // TODO: Maybe put different LUTs there?
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
