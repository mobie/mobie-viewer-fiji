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
package de.embl.cba.mobie2.transform;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.HashMap;

public class CroppedSource< T extends NumericType<T> > implements Source<T>
{
    private final Source<T> source;
    private final String name;
    private final RealInterval crop;
    private final boolean zeroMin;

    protected transient final DefaultInterpolators< T > interpolators;
    private transient HashMap< Integer, Interval > levelToVoxelInterval;

    public CroppedSource( Source< T > source, RealInterval crop, boolean zeroMin )
    {
        this.source = source;
        this.name = null;
        this.crop = crop;
        this.zeroMin = zeroMin;
        this.interpolators = new DefaultInterpolators();

        initCropIntervals( source, crop );
    }

    public CroppedSource( Source< T > source, String name, RealInterval crop, boolean zeroMin )
    {
        this.source = source;
        this.name = name;
        this.crop = crop;
        this.zeroMin = zeroMin;
        this.interpolators = new DefaultInterpolators();

        initCropIntervals( source, crop );
    }


    private void initCropIntervals( Source< T > source, RealInterval crop )
    {
        final AffineTransform3D transform3D = new AffineTransform3D();
        levelToVoxelInterval = new HashMap<>();
        for ( int l = 0; l < source.getNumMipmapLevels(); l++ )
        {
            source.getSourceTransform( 0, l, transform3D );
            final Interval voxelInterval = Intervals.smallestContainingInterval( transform3D.inverse().estimateBounds( crop ) );
            levelToVoxelInterval.put( l, voxelInterval );
        }
    }

    public Source< ? > getWrappedSource() {
        return source;
    }

    @Override
    public boolean isPresent(int t) {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< T > getSource(int t, int level)
    {
        final IntervalView< T > intervalView = Views.interval( source.getSource( t, level ), levelToVoxelInterval.get( level ) );

        if ( zeroMin )
            return Views.zeroMin( intervalView );
        else
            return intervalView;
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method)
    {
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                zeroExtendedCrop = Views.extendZero( getSource( t, level ) );
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( zeroExtendedCrop, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform)
    {
        source.getSourceTransform( t, level, transform );
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
