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
package org.embl.mobie.viewer.source;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractBoundarySource< T > implements Source< T >, SourceWrapper< T >
{
    protected final Source< T > source;
    protected boolean showAsBoundaries;
    protected float boundaryWidth;
    protected ArrayList< Integer > boundaryDimensions;

    public AbstractBoundarySource( final Source< T > source )
    {
        this.source = source;
    }

    public void showAsBoundary( boolean showAsBoundaries, float boundaryWidth ) {
        this.showAsBoundaries = showAsBoundaries;
        this.boundaryWidth = boundaryWidth;
        this.boundaryDimensions = boundaryDimensions();
    }

    @Override
    public boolean doBoundingBoxCulling() {
        return source.doBoundingBoxCulling();
    }

    @Override
    public synchronized void getSourceTransform(final int t, final int level, final AffineTransform3D transform) {
        source.getSourceTransform(t, level, transform);
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< T > getSource( final int t, final int level )
    {
        return source.getSource( t, level );
    }

    protected ArrayList< Integer > boundaryDimensions()
    {
        final ArrayList< Integer > dimensions = new ArrayList<>();
        if ( bounds != null )
        {
            // check whether the source is effectively 2D,
            // i.e. much thinner along one dimension than the
            // requested boundary width
            for ( int d = 0; d < 3; d++ )
            {
                final double sourceBound = Math.abs( bounds.realMax( d ) - bounds.realMin( d ) );
                if ( sourceBound > 3 * boundaryWidth )
                    dimensions.add( d );
            }
        }
        else if ( source.getSource( 0,0 ).dimension( 2 ) == 1 )
        {
            // 2D source
            dimensions.add( 0 );
            dimensions.add( 1 );
        }
        else
        {
            // 3D source
            dimensions.add( 0 );
            dimensions.add( 1 );
            dimensions.add( 2 );
        }

        return dimensions;
    }

    protected float[] getBoundarySize( int t, int level )
    {
        final float[] boundaries = new float[ 3 ];
        Arrays.fill( boundaries, (float) boundaryWidth );
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

    public boolean isShowAsBoundaries()
    {
        return showAsBoundaries;
    }

    public float getBoundaryWidth()
    {
        return boundaryWidth;
    }
}
