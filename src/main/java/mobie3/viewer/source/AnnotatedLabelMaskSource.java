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
package mobie3.viewer.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mobie3.viewer.annotation.Segment;
import mobie3.viewer.annotation.SegmentProvider;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.IntegerType;

public class AnnotatedLabelMaskSource< T extends IntegerType< T >, S extends Segment > extends AbstractSourceWrapper< T, AnnotationType< S > >
{
    private final SegmentProvider< S > segmentProvider;

    public AnnotatedLabelMaskSource( final Source< T > source, SegmentProvider< S > segmentProvider )
    {
        super( source );
        this.segmentProvider = segmentProvider;
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< AnnotationType< S > > getSource( final int t, final int level )
    {
        return Converters.convert( source.getSource( t, level ), ( input, output ) -> {
            set( input, t, output );
        }, new AnnotationType( segmentProvider.createVariable() ) );
    }

    @Override
    public RealRandomAccessible< AnnotationType< S > > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra,
                ( T input, AnnotationType< S > output ) ->
                set( input, t, output ),
                new AnnotationType<>() );
    }

    private void set( T input, int t, AnnotationType< S > output  )
    {
        // TODO: Create SegmentId! Could be a static method in Segment
        final S segment = segmentProvider.getSegment( input.getInteger(), t, source.getName() );
        final AnnotationType< S > segmentType = new AnnotationType( segment );
        output.set( segmentType );
    }

    @Override
    public AnnotationType< S > getType()
    {
        return new AnnotationType( segmentProvider.createVariable() );
    }
}
