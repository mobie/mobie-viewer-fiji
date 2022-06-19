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

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.tables.imagesegment.ImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.segment.SegmentAdapter;

import java.util.Collection;
import java.util.List;

public class SegmentSource< T extends NumericType< T > & RealType< T >, V extends Volatile< T >, I extends ImageSegment > implements Source< VolatileSegmentType >, SourceWrapper< T >
{
    private final Source< T > source;
    private final Collection< Integer > timepoints;
    private final SegmentAdapter< I > adapter;

    public SegmentSource( final Source< T > source, final List< I > tableRows )
    {
        this.source = source;
        this.adapter = new SegmentAdapter<>( tableRows );
        this.timepoints = null;
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
        if ( timepoints != null )
            return timepoints.contains( t );
        else
            return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< VolatileSegmentType > getSource( final int t, final int level )
    {
        return Converters.convert( source.getSource( t, level ), ( input, output ) -> {
            set( t, output, input );
        }, new VolatileSegmentType() );
    }


    @Override
    public RealRandomAccessible< VolatileSegmentType > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra, ( input, output ) -> set( t, output, input ), new VolatileSegmentType() );
    }

    private void set( int t, VolatileSegmentType output, T input )
    {
        output.set( new VolatileSegmentType( adapter.getSegment( input.getRealDouble(), t, source.getName() ) ) );
    }

    @Override
    public VolatileSegmentType getType() {
        return new VolatileSegmentType();
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
}
