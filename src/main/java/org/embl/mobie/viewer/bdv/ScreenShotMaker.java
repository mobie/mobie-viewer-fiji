/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import org.embl.mobie.viewer.bdv.render.AccumulateOccludingProjectorARGB;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
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
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
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
    private final BdvHandle bdvHandle;
    private double physicalPixelSpacingInXY = 1;
    private String physicalUnit = "Pixels";
    private boolean sourceInteractionWithViewerPlaneOnly2D = false; // TODO: maybe remove in the future
    ImagePlus screenShot = null;
    private CompositeImage rawImageData = null;
    private final SourceAndConverterBdvDisplayService displayService;
    //private final ISourceAndConverterService sacService;
    private long captureWidth;
    private long captureHeight;

    public ScreenShotMaker( BdvHandle bdvHandle) {
        this.bdvHandle = bdvHandle;
        this.displayService = SourceAndConverterServices.getBdvDisplayService();
        //this.sacService = SourceAndConverterServices.getSourceAndConverterService();
    }

    public void setPhysicalPixelSpacingInXY(double spacing, String unit) {
        this.screenShot = null;
        this.physicalPixelSpacingInXY = spacing;
        this.physicalUnit = unit;
    }

    public void setSourceInteractionWithViewerPlaneOnly2D(boolean sourceInteractionWithViewerPlaneOnly2D) {
        this.screenShot = null;
        this.sourceInteractionWithViewerPlaneOnly2D = sourceInteractionWithViewerPlaneOnly2D;
    }

    private void process() {
        if (screenShot != null) {
            return;
        }
        createScreenShot();
    }

    public ImagePlus getRgbScreenShot() {
        process();
        return screenShot;
    }

    public CompositeImage getRawScreenShot()
    {
        process();
        return rawImageData;
    }

    private void createScreenShot()
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( viewerTransform );

        final double viewerVoxelSpacing = getViewerVoxelSpacing( bdvHandle );
        double dxy = physicalPixelSpacingInXY / viewerVoxelSpacing;

        final int w = bdvHandle.getViewerPanel().getWidth();
        final int h = bdvHandle.getViewerPanel().getHeight();

        captureWidth = ( long ) Math.ceil( w / dxy );
        captureHeight = ( long ) Math.ceil( h / dxy );

        final ArrayList< RandomAccessibleInterval< UnsignedShortType > > rawCaptures = new ArrayList<>();
        final ArrayList< RandomAccessibleInterval< ARGBType > > argbSources = new ArrayList<>();
        final ArrayList< ARGBType > colors = new ArrayList<>();

        final ArrayList< double[] > displayRanges = new ArrayList<>();

        final List< SourceAndConverter <?> > visibleSacs = getVisibleSacs( bdvHandle );
        if ( visibleSacs.size() == 0 ) return;

        List< SourceAndConverter< ? > > sacs = new ArrayList<>();
        for ( SourceAndConverter< ?  > sac : visibleSacs )
        {
            if ( !isSourceIntersectingCurrentView( bdvHandle, sac.getSpimSource(), sourceInteractionWithViewerPlaneOnly2D ) )
                continue;
            sacs.add( sac );
        }
        if ( sacs.size() == 0 ) return;

        final int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

        for ( SourceAndConverter< ?  > sac : sacs )
        {
            final RandomAccessibleInterval< UnsignedShortType > rawCapture
                    = ArrayImgs.unsignedShorts( captureWidth, captureHeight );
            final RandomAccessibleInterval< ARGBType > argbCapture
                    = ArrayImgs.argbs( captureWidth, captureHeight );

            Source< ? > source = sac.getSpimSource();
            final Converter converter = sac.getConverter();

            final int level = getLevel( source, physicalPixelSpacingInXY );
            final AffineTransform3D sourceTransform =
                    BdvHandleHelper.getSourceTransform( source, t, level );

            AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
            viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
            viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

            // TODO: Once we have a logic for segmentation images, make this choice depend on this
            boolean interpolate = true;

            Grids.collectAllContainedIntervals(
                    Intervals.dimensionsAsLongArray( argbCapture ),
                    new int[]{100, 100}).parallelStream().forEach( interval ->
            {
                RealRandomAccess< ? extends RealType< ? > > realTypeAccess =
                        getRealTypeRealRandomAccess( t, source, level, interpolate );
                RealRandomAccess< ? > access =
                        getRealRandomAccess( t, source, level, interpolate );

                // to collect raw data
                final IntervalView< UnsignedShortType > rawCrop = Views.interval( rawCapture, interval );
                final Cursor< UnsignedShortType > rawCaptureCursor = Views.iterable( rawCrop ).localizingCursor();
                final RandomAccess< UnsignedShortType > rawCaptureAccess = rawCrop.randomAccess();

                // to collect coloured data
                final IntervalView< ARGBType > argbCrop = Views.interval( argbCapture, interval );
                final RandomAccess< ARGBType > argbCaptureAccess = argbCrop.randomAccess();

                final double[] canvasPosition = new double[ 3 ];
                final double[] sourceRealPosition = new double[ 3 ];

                final ARGBType argbType = new ARGBType();

                while ( rawCaptureCursor.hasNext() )
                {
                    rawCaptureCursor.fwd();
                    rawCaptureCursor.localize( canvasPosition );
                    rawCaptureAccess.setPosition( rawCaptureCursor );
                    argbCaptureAccess.setPosition( rawCaptureCursor );

                    // canvasPosition is the position on the canvas, in calibrated units
                    // dxy is the step size that is needed to get the desired resolution in the
                    // output image
                    canvasPosition[ 0 ] *= dxy;
                    canvasPosition[ 1 ] *= dxy;

                    viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );

                    setRawCapturePixelValue( realTypeAccess, rawCaptureAccess, sourceRealPosition );
                    setArgbCapturePixelValue( converter, access, argbCaptureAccess, sourceRealPosition, argbType );
                }
            });

            rawCaptures.add( rawCapture );
            argbSources.add( argbCapture );
            // colors.add( getSourceColor( bdv, sourceIndex ) ); Not used, show GrayScale
            displayRanges.add( BdvHandleHelper.getDisplayRange( displayService.getConverterSetup( sac ) ) );
        }

        final double[] voxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 2; d++ )
            voxelSpacing[ d ] = physicalPixelSpacingInXY;

        voxelSpacing[ 2 ] = viewerVoxelSpacing; // TODO: What to put here?

        if ( rawCaptures.size() > 0 )
        {
            screenShot = createImagePlus( physicalUnit, argbSources, voxelSpacing, sacs );
            rawImageData  = createCompositeImage( voxelSpacing, physicalUnit, rawCaptures, colors, displayRanges );
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

    private void setArgbCapturePixelValue( Converter converter, RealRandomAccess< ? > access, RandomAccess< ARGBType > argbCaptureAccess, double[] sourceRealPosition, ARGBType argbType )
    {
        access.setPosition( sourceRealPosition );
        final Object pixelValue = access.get();
        if ( pixelValue instanceof ARGBType )
            argbType.set( ( ARGBType ) pixelValue );
        else
            converter.convert( pixelValue, argbType );

        final int sourceARGBIndex = argbType.get();
        final int captureARGBIndex = argbCaptureAccess.get().get();

        // here the projection happens
        int a = ARGBType.alpha( sourceARGBIndex ) + ARGBType.alpha( captureARGBIndex );
        int r = ARGBType.red( sourceARGBIndex ) + ARGBType.red( captureARGBIndex );
        int g = ARGBType.green( sourceARGBIndex )+ ARGBType.green( captureARGBIndex );
        int b = ARGBType.blue( sourceARGBIndex )+ ARGBType.blue( captureARGBIndex );

        if ( a > 255 )
            a = 255;
        if ( r > 255 )
            r = 255;
        if ( g > 255 )
            g = 255;
        if ( b > 255 )
            b = 255;

        argbCaptureAccess.get().set( ARGBType.rgba( r, g, b, a ) );
    }

    private void setRawCapturePixelValue( RealRandomAccess< ? extends RealType< ? > > realTypeAccess, RandomAccess< UnsignedShortType > realCaptureAccess, double[] sourceRealPosition )
    {
        realTypeAccess.setPosition( sourceRealPosition );
        final RealType< ? > realType = realTypeAccess.get();
        realCaptureAccess.get().setReal( realType.getRealDouble() );
    }

    private RealRandomAccess< ? > getRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        RealRandomAccess< ? > access;
        if ( interpolate )
            access = source.getInterpolatedSource( t, level, Interpolation.NLINEAR ).realRandomAccess();
        else
            access = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ).realRandomAccess();
        return access;
    }

    private ImagePlus createImagePlus(
            String physicalUnit,
            ArrayList< RandomAccessibleInterval< ARGBType > > argbSources,
            double[] voxelSpacing,
            List< SourceAndConverter< ? > > sacs )
    {
        final RandomAccessibleInterval< ARGBType > argbTarget = ArrayImgs.argbs( captureWidth, captureHeight );

        project( argbSources, argbTarget, sacs );

        return asImagePlus( argbTarget, physicalUnit, voxelSpacing );
    }

    private void project( ArrayList< RandomAccessibleInterval< ARGBType > > argbSources, RandomAccessibleInterval< ARGBType > argbTarget, List< SourceAndConverter< ? > > sacs )
    {
        final Cursor< ARGBType > argbCursor = Views.iterable( argbTarget ).localizingCursor();
        final int numVisibleSources = argbSources.size();
        Cursor< ARGBType >[] cursors = getCursors( argbSources, numVisibleSources );
        final ArrayList< ArrayList< Integer > > occlusions = AccumulateOccludingProjectorARGB.getOcclusions( sacs );

        while ( argbCursor.hasNext() )
        {
            try
            {
                argbCursor.fwd();
                for ( int i = 0; i < numVisibleSources; i++ )
                    cursors[ i ].fwd();
                final int argbIndex = AccumulateOccludingProjectorARGB.getArgbIndex( cursors, occlusions );
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
        final ImagePlus rgbImage = ImageJFunctions.wrap( argbCapture, "View Capture RGB" );

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

        final ImagePlus imp = ImageJFunctions.wrap( stack, "View Capture Raw" );

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

        compositeImage.setTitle( "View Capture Raw" );
        return compositeImage;
    }

    public static RealRandomAccess< ? extends RealType< ? > >
    getRealTypeRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        if ( interpolate )
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NLINEAR).realRandomAccess();
        else
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR).realRandomAccess();
    }
}
