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
package org.embl.mobie.lib.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.view.Views;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RealTransformedSource<T> implements Source<T>, MipmapOrdering, SourceWrapper< T >
{
    private final Source<T> source;

    private final String name;

    private final MipmapOrdering sourceMipmapOrdering;

    private final RealTransform realTransform;

    private final BoundingBoxEstimation boundingBoxEstimation =
            new BoundingBoxEstimation( BoundingBoxEstimation.Method.FACES, 5 );

    private final Map< Long, Interval > boundingIntervalCache = new ConcurrentHashMap<>();

    public RealTransformedSource(
            final Source<T> source,
            final RealTransform realTransform,
            final String name) {

        this.source = source;
        this.name = name;
        this.realTransform = realTransform;
        sourceMipmapOrdering =
                source instanceof MipmapOrdering ?
                        ( MipmapOrdering ) source : null;
    }

    @Override
    public boolean isPresent(final int t) {

        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource( final int t, final int level ) {
        final Interval interval = boundingIntervalCache.computeIfAbsent(
                cacheKey( t, level ),
                key -> estimateBoundingInterval( t, level ) );

        return Views.interval(
                Views.raster( getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ) ),
                interval );
    }

    private static long cacheKey( final int t, final int level )
    {
        return ( ( long ) t << 32 ) | ( level & 0xffffffffL );
    }

    private Interval estimateBoundingInterval( final int t, final int level ) {

        final AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( t, level, sourceTransform );

        final InvertibleRealTransform invertible;
        if ( realTransform instanceof InvertibleRealTransform )
            invertible = ( ( InvertibleRealTransform ) realTransform ).copy();
        else
            invertible = new WrappedIterativeInvertibleRealTransform<>( realTransform.copy() );

        if ( invertible instanceof WrappedIterativeInvertibleRealTransform )
        {
            @SuppressWarnings( "rawtypes" )
            final WrappedIterativeInvertibleRealTransform wrapped =
                    ( WrappedIterativeInvertibleRealTransform ) invertible;
            wrapped.getOptimzer().setMaxStep( 500.0 );
        }

        // Map output local voxel coordinates to wrapped-source local voxel coordinates.
        final RealTransformSequence inverseChain = new RealTransformSequence();
        inverseChain.add( sourceTransform );
        inverseChain.add( invertible.inverse() );
        inverseChain.add( sourceTransform.inverse() );

        return boundingBoxEstimation.estimatePixelInterval( inverseChain, source.getSource( t, level ) );
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(
            final int t,
            final int level,
            final Interpolation method)
    {
        // When painting a pixel in global space,
        // BigDataViewer will take this RealRandomAccessible
        // and access the voxel value through the inverse of the source transform.
        // (the source transform goes from voxel to global space and BigDataViewer always inverts that).
        //
        // Now, all the below code to sneak in another transformation in real (global) space.

        RealRandomAccessible< T > interpolatedSource = source.getInterpolatedSource( t, level, method );

        final AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( t, level, sourceTransform );

        final RealTransformSequence totalTransform = new RealTransformSequence();
        totalTransform.add( sourceTransform ); // Negate the inverse sourceTransform that BDV will apply to stay in physical space because the realtransform is given in physical space
        totalTransform.add( realTransform.copy() ); // Apply real transform in physical space
        totalTransform.add( sourceTransform.inverse() ); // Go into voxel space to fetch the pixel

        return new RealTransformRealRandomAccessible<>( interpolatedSource, totalTransform );
    }

    @Override
    public void getSourceTransform(final int t, final int level, final AffineTransform3D transform)
    {
        source.getSourceTransform( t, level, transform );
    }

    @Override
    public T getType() {

        return source.getType();
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {

        return source.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {

        return source.getNumMipmapLevels();
    }

    @Override
    public synchronized MipmapHints getMipmapHints(
            final AffineTransform3D screenTransform,
            final int timepoint,
            final int previousTimepoint) {

        if ( sourceMipmapOrdering != null )
            return sourceMipmapOrdering.getMipmapHints(
                    screenTransform,
                    timepoint,
                    previousTimepoint);

        return new DefaultMipmapOrdering( source ).getMipmapHints(
                screenTransform,
                timepoint,
                previousTimepoint );
    }

    @Override
    public Source< T > getWrappedSource()
    {
        return source;
    }

    @Override
    public boolean doBoundingBoxCulling()
    {
        return false;
    }

    public RealTransform getRealTransform()
    {
        return realTransform;
    }
}
