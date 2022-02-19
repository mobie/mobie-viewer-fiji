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
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.source.SourceWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaskedSource< T extends NumericType<T> > implements Source< T >, SourceWrapper< T >
{
    // Serialisation
    private Source< T > source;
    private final String name;
    private final double[] maskMin;
    private final double[] maskMax;
    private final AffineTransform3D maskTransform; // maskInterval to physical
    private final boolean center;

    // Runtime
    protected transient final DefaultInterpolators< T > interpolators;
    private transient HashMap< Integer, Interval > levelToVoxelInterval;
    private final transient Map< Integer, RealMaskRealInterval > dataMasks;
    private final transient Map< Integer, AffineTransform3D > sourceTransforms;
    private final transient T type;

    public MaskedSource( Source< T > source, String name, double[] maskMin, double[] maskMax, AffineTransform3D maskTransform, boolean center  )
    {
        this.source = source;
        this.name = name;
        this.maskMin = maskMin;
        this.maskMax = maskMax;
        this.maskTransform = maskTransform; 
        this.center = center;
        this.type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
        this.interpolators = new DefaultInterpolators();

        dataMasks = new ConcurrentHashMap<>();
        sourceTransforms = new ConcurrentHashMap<>();

        for ( int level = 0; level < getNumMipmapLevels(); level++ )
        {
            final AffineTransform3D sourceTransform = new AffineTransform3D();
            source.getSourceTransform( 0, level, sourceTransform );

            final RealMaskRealInterval physicalMask = GeomMasks.closedBox( maskMin, maskMax ).transform( maskTransform.inverse() );
            final RealMaskRealInterval dataMask = physicalMask.transform( sourceTransform.copy() );

            dataMasks.put( level, dataMask );

            // copy the original source transforms, because they
            // may be altered, e.g., by a manual transform
            sourceTransforms.put( level, sourceTransform );
        }
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
        final FunctionRandomAccessible< T > maskedRA = new FunctionRandomAccessible< T >(
                3,
                ( dataCoordinates, value ) -> {
                    if ( dataMask.test( dataCoordinates ) )
                        value.set( rai.getAt( dataCoordinates ) );
                    else
                        value.setZero();
                },
                () -> type.createVariable() );

        // TODO: here we could change the interval to reflect the crop!
        return Views.interval( maskedRA, rai );
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
    {
        // interpolate the masked rai
        final RandomAccessibleInterval< T > rai = getSource( t, level );
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >> extendedRai = Views.extendZero( rai );
        RealRandomAccessible< T > rra = Views.interpolate( extendedRai, interpolators.get(method) );
        return rra;
    }

    @Override
    public boolean doBoundingBoxCulling()
    {
        return source.doBoundingBoxCulling();
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

}
