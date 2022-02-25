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
package org.embl.mobie.viewer.transform;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.source.SourceWrapper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.embl.mobie.viewer.transform.TransformHelpers.extractRectifyAffineTransform3D;
import static org.embl.mobie.viewer.transform.TransformHelpers.getCenter;

public class MaskedSource< T extends NumericType<T> > implements Source< T >, SourceWrapper< T >
{
    private final RealMaskRealInterval physicalMask;
    private final boolean rectify;
    private final boolean center;
    private Source< T > source;
    private final String name;
    private final RealInterval maskInterval;
    private final AffineTransform3D maskToPhysicalTransform;
    protected transient final DefaultInterpolators< T > interpolators;
    private final transient Map< Integer, RealMaskRealInterval > dataMasks;
    private final transient Map< Integer, AffineTransform3D > sourceTransforms;
    private final transient Map< Integer, AffineTransform3D > rotationTransforms;
    private final transient T type;

    public MaskedSource( Source< T > source, String name, RealInterval maskInterval, AffineTransform3D maskToPhysicalTransform, boolean rectify, boolean center )
    {
        this.source = source;
        this.name = name;
        this.maskInterval = maskInterval;
        this.maskToPhysicalTransform = maskToPhysicalTransform;
        this.type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
        this.rectify = rectify;
        this.center = center;
        this.interpolators = new DefaultInterpolators();

        physicalMask = GeomMasks.closedBox( maskInterval.minAsDoubleArray(), maskInterval.maxAsDoubleArray() ).transform( maskToPhysicalTransform );

        dataMasks = new ConcurrentHashMap<>();
        sourceTransforms = new ConcurrentHashMap<>();
        rotationTransforms = new ConcurrentHashMap<>();

        for ( int level = 0; level < getNumMipmapLevels(); level++ )
        {
            final AffineTransform3D sourceTransform = new AffineTransform3D();
            source.getSourceTransform( 0, level, sourceTransform );

            final AffineTransform3D maskToDataTransform = maskToPhysicalTransform.copy().preConcatenate( sourceTransform.inverse() );

            final RealMaskRealInterval dataMask = GeomMasks.closedBox( maskInterval.minAsDoubleArray(), maskInterval.maxAsDoubleArray() ).transform( maskToDataTransform.inverse() );

            dataMasks.put( level, dataMask );

            if ( rectify )
            {
                final AffineTransform3D rotateAroundMaskCenter = TransformHelpers.getRectifyAffineTransform3D( maskInterval, maskToPhysicalTransform );
                sourceTransform.preConcatenate( rotateAroundMaskCenter );
            }

            if ( center )
            {
                final double[] maskPhysicalCenter = getCenter( maskToPhysicalTransform.estimateBounds( maskInterval ) );
                final AffineTransform3D translateToOrigin = new AffineTransform3D();
                translateToOrigin.translate( Arrays.stream( maskPhysicalCenter ).map( x -> -x ).toArray()  );
                sourceTransform.preConcatenate( translateToOrigin );
            }

            // TODO: depending on whether the mask is now centered
            //  one may have to rotate around the center?
//            final AffineTransform3D rectifyAffineTransform3D = extractRectifyAffineTransform3D( sourceTransform );
//            sourceTransform.preConcatenate( rectifyAffineTransform3D );
//            rotationTransforms.put( level, rectifyAffineTransform3D );

            // copy the original source transforms, because they
            // may be altered, e.g., by a manual transform
            sourceTransforms.put( level, sourceTransform );
        }

        final FinalRealInterval bounds = TransformHelpers.estimateBounds( this );
    }

    public Source< T > getWrappedSource() {
        return source;
    }

    @Override
    public boolean isPresent(int t) {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< T > getSource(int t, int level)
    {
        final RandomAccessibleInterval< T > rai = source.getSource( t, level );

        final RealMaskRealInterval dataMask = dataMasks.get( level );

        // apply mask in data space
        final FunctionRandomAccessible< T > maskedRA = new FunctionRandomAccessible< T >(
                3,
                ( dataCoordinates, value ) -> {
                    if ( dataMask.test( dataCoordinates ) )
                        value.set( rai.getAt( dataCoordinates ) );
                    else
                        value.setZero();
                },
                () -> type.createVariable() );

        // crop interval
        // generally larger than the mask,
        // because the box may lie oblique in data space
        Interval dataInterval = Intervals.smallestContainingInterval( dataMask );

        // TODO: not sure whether below intersect would help...
        // Maybe if the source is 2D and the crop in 3D is much larger?
//        if ( ! Intervals.contains( rai, dataInterval ) )
//        {
//            dataInterval = Intervals.intersect( rai, dataInterval );
//        }

        final IntervalView< T > interval = Views.interval( maskedRA, dataInterval );
        return interval;
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, method );

        final RealMaskRealInterval dataMask = dataMasks.get( level );

        RealRandomAccessible< T > maskedRra = new FunctionRealRandomAccessible< T >(
                3,
                ( dataCoordinates, value ) -> {
                    if ( dataMask.test( dataCoordinates ) )
                        value.set( rra.getAt( dataCoordinates ) );
                    else
                        value.setZero();
                },
                () -> type.createVariable() );

        return maskedRra;
    }

    @Override
    public boolean doBoundingBoxCulling()
    {
        return true;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform)
    {
        transform.set( sourceTransforms.get( level ) );
    }

    @Override
    public T getType() {
        return source.getType();
    }

    @Override
    public String getName()
    {
        if ( name != null ) return name;
        else return source.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return source.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return source.getNumMipmapLevels();
    }

    public RealInterval getMaskInterval()
    {
        return maskInterval;
    }

    public AffineTransform3D getMaskToPhysicalTransform()
    {
        return maskToPhysicalTransform;
    }

    public boolean isRectify()
    {
        return rectify;
    }

    public boolean isCenter()
    {
        return center;
    }
}
