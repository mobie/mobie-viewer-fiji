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

import bdv.util.Affine3DHelpers;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractBoundarySource< T > implements Source< T >, SourceWrapper< T >
{
    protected final Source< T > source;
    protected boolean showAsBoundaries;
    protected double boundaryWidth;
    protected ArrayList< Integer > boundaryDimensions;
    protected RealInterval bounds;

    public AbstractBoundarySource( final Source< T > source, boolean showAsBoundaries, float boundaryWidth, @Nullable RealInterval bounds )
    {
        this.source = source;
        this.showAsBoundaries = showAsBoundaries;
        this.boundaryWidth = boundaryWidth;
        this.bounds = bounds;
        this.boundaryDimensions = boundaryDimensions();
    }

    public void showAsBoundary( boolean showAsBoundaries, double boundaryWidth )
    {
        this.showAsBoundaries = showAsBoundaries;
        this.boundaryWidth = boundaryWidth; // in calibrated units
        this.boundaryDimensions = boundaryDimensions();
    }

    @Override
    public boolean doBoundingBoxCulling() {
        return source.doBoundingBoxCulling();
    }

    @Override
    public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
    {
        source.getSourceTransform( t, level, transform );
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent( t );
    }

    @Override
    public RandomAccessibleInterval< T > getSource( final int t, final int level )
    {
        return source.getSource( t, level );
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        if ( showAsBoundaries  )
        {
            // Ultimately we need the boundaries in pixel units,
            // because we have to check the voxel values in the rra,
            // which is in pixel units.
            // TODO: the above statement does not seem true for a RegionAnnotationImage
            //   which is a RealRandomAccessible.
            //   Maybe there could be different mechanisms for creating
            //   boundaries for voxel based sources and for the RegionAnnotationImage?
            // However, it feels like we could stay longer
            // in physical units here to make this less confusing.
            final double[] pixelUnitsBoundaryWidth = pixelBoundaryWidth( t, level );
            return createBoundaryRealRandomAccessible( rra, boundaryDimensions, pixelUnitsBoundaryWidth );
        }
        else
        {
            return rra;
        }
    }

    // Note that it can make sense to specify the boundary width
    // as floats (even though typically sources are
    // associated to some pixel grid).
    // For example, if the underlying source is a RealRandomAccessibleSource,
    // the values could be directly created in real space without
    // any backing of a voxel grid. This is in fact the case for the
    // ImageAnnotationLabelImage, which is one use-case of the BoundarySource.
    protected abstract RealRandomAccessible< T > createBoundaryRealRandomAccessible( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, double[] pixelUnitsBoundaryWidth );

    protected ArrayList< Integer > boundaryDimensions()
    {
        final ArrayList< Integer > dimensions = new ArrayList<>();

        if ( source.getSource( 0,0 ).dimension( 2 ) == 1 )
        {
            // 2D source
            dimensions.add( 0 );
            dimensions.add( 1 );
            return dimensions;
        }

        if ( bounds != null )
        {
            // check whether the source is effectively 2D,
            // i.e. much thinner along one dimension than the
            // requested boundary width
            for ( int d = 0; d < 3; d++ )
            {
                final double sourceWidth = Math.abs( bounds.realMax( d ) - bounds.realMin( d ) );
                if ( sourceWidth > 3 * boundaryWidth )
                    dimensions.add( d );
            }
            return dimensions;
        }

        // 3D source
        dimensions.add( 0 );
        dimensions.add( 1 );
        dimensions.add( 2 );
        return dimensions;

    }

    protected double[] pixelBoundaryWidth( int t, int level )
    {
        final double[] boundaries = new double[ 3 ];
        Arrays.fill( boundaries, boundaryWidth );
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        getSourceTransform( t, level, sourceTransform );
        for ( int d = 0; d < 3; d++ )
        {
            final double scale = Affine3DHelpers.extractScale( sourceTransform, d );
            boundaries[ d ] /= scale;
        }
        return boundaries;
    }

    @Override
    public T getType() {
        return source.getType();
    }

    @Override
    public String getName() {
        return source.getName();
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
    public Source<T> getWrappedSource() {
        return source;
    }

    public boolean showAsBoundaries()
    {
        return showAsBoundaries;
    }

    public double getBoundaryWidth()
    {
        return boundaryWidth;
    }

}
