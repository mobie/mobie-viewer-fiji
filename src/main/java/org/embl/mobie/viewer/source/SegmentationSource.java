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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.segment.SegmentAdapter;

import java.util.Collection;


// TODO: Does I really need to extend ImageSegment here?
public class SegmentationSource< T extends NumericType< T > & RealType< T >, I extends ImageSegment > extends AbstractSourceWrapper< T, AnnotationType< I > >
{
    private final SegmentAdapter< I > adapter;

    public SegmentationSource( final Source< T > source, SegmentAdapter< I > adapter )
    {
        super( source );
        this.adapter = adapter;
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< AnnotationType< I >  > getSource( final int t, final int level )
    {
        return Converters.convert( source.getSource( t, level ), ( input, output ) -> {
            set( input, t, output );
        }, new SegmentType() );
    }

    @Override
    public RealRandomAccessible< AnnotationType< I >  > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra,
                ( T input, AnnotationType< I > output ) ->
                set( input, t, output ),
                new SegmentType() );
    }

    private void set( T input, int t, AnnotationType< I > output  )
    {
        final I segment = adapter.getSegment( input.getRealDouble(), t, source.getName() );
        final SegmentType< I > segmentType = new SegmentType( segment );
        output.set( segmentType );
    }

    @Override
    public SegmentType getType() {
        return new SegmentType();
    }
}
