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
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.HashMap;

public class MaskedSource< T extends NumericType<T> > implements Source< T > //, Function< Source< T >, Source< T > >
{
    private Source< T > source;
    private final String name;
    private final RealMaskRealInterval mask;
    private final boolean center;

    protected transient final DefaultInterpolators< T > interpolators;
    private transient HashMap< Integer, Interval > levelToVoxelInterval;

    // TODO: add affine transform to orient the crop
    public MaskedSource( Source< T > source, String name, RealMaskRealInterval mask, boolean center )
    {
        this.source = source;
        this.name = name;
        this.mask = mask;
        this.center = center;
        this.interpolators = new DefaultInterpolators();

        // TODO Do we need this? It could be nice for the bounding box culling
        initVoxelCropIntervals( source, mask );
    }

    private void initVoxelCropIntervals( Source< T > source, RealInterval crop )
    {
        final AffineTransform3D transform3D = new AffineTransform3D();
        levelToVoxelInterval = new HashMap<>();
        final int numMipmapLevels = source.getNumMipmapLevels();
        for ( int level = 0; level < numMipmapLevels; level++ )
        {
            source.getSourceTransform( 0, level, transform3D );
            final AffineTransform3D inverse = transform3D.inverse();
            final Interval voxelInterval = Intervals.smallestContainingInterval( inverse.estimateBounds( crop ) );
            final FinalInterval containedVoxelInterval = intersectWithSourceInterval( source, crop, level, voxelInterval );
            levelToVoxelInterval.put( level, containedVoxelInterval );
        }
    }

    private FinalInterval intersectWithSourceInterval( Source< T > source, RealInterval crop, int level, Interval voxelInterval )
    {
        // If the interval is outside the bounds of the RAI then there is nothing to show.
        // Moreover the fetcher threads throw errors when trying to access pixels outside the RAI.
        // Thus let's limit the interval to where there actually is data.
        final RandomAccessibleInterval< T > rai = source.getSource( 0, level );
        final FinalInterval intersect = Intervals.intersect( rai, voxelInterval );
        if ( Intervals.numElements( intersect ) <= 0 )
        {
            throw new RuntimeException( "The crop interval " + crop + " is not within the image source " + source.getName() );
        }
        return intersect;
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
        return source.getSource( t, level );
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
    {
        final RandomAccessibleInterval< T > rai = getSource( t, level );
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >> extendedRai = Views.extendZero( rai );
        RealRandomAccessible< T > rra = Views.interpolate( extendedRai, interpolators.get(method) );

        // sourceTransform: data space (of rra) to physical space
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( t, level, sourceTransform );

        final T type = Util.getTypeFromInterval( rai );

        final FunctionRealRandomAccessible< T > realRandomAccessible = new FunctionRealRandomAccessible< T >(
                3,
                ( dataCoordinates, value ) -> {

                    final RealPoint physicalCoordinates = new RealPoint( 3 );
                    sourceTransform.apply( dataCoordinates, physicalCoordinates );

                    if ( mask.test( physicalCoordinates ) )
                        value.set( rra.getAt( dataCoordinates ) );
                    else
                        value.setZero();

                },
                () -> type.createVariable() );

        return realRandomAccessible;
    }

    @Override
    public boolean doBoundingBoxCulling()
    {
        return true;
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
