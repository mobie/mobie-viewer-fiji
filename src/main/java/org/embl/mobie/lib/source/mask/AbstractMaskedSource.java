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
package org.embl.mobie.lib.source.mask;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.source.SourceWrapper;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractMaskedSource< T > implements Source< T >, SourceWrapper< T >
{
    protected final Source< T > source;
    private final RealMaskRealInterval mask;

    public AbstractMaskedSource( Source< T > source, RealMaskRealInterval mask )
    {
        this.source = source;
        this.mask = mask;
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
        // FIXME also implement masking here
        return source.getSource( t, level );
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );
        AffineTransform3D affineTransform3D = new AffineTransform3D();
        source.getSourceTransform( t, level, affineTransform3D );
        RealMaskRealInterval transformed = mask.transform( affineTransform3D.inverse() );
        return createMaskedRealRandomAccessible( rra, transformed );
    }

    protected abstract RealRandomAccessible< T > createMaskedRealRandomAccessible( RealRandomAccessible< T > rra, RealMaskRealInterval mask );

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
