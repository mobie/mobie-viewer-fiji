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
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.segment.SegmentAdapter;

import java.util.Collection;

public class VolatileSegmentationSource< T extends NumericType< T > & RealType< T >, V extends Volatile< T >, I extends ImageSegment > extends AbstractSourceWrapper< V, VolatileAnnotationType< I > >
{
    private final SegmentAdapter< I > adapter;

    public VolatileSegmentationSource( final Source< V > source, SegmentAdapter< I > adapter )
    {
        super( source );
        this.adapter = adapter;
    }

    @Override
    public RandomAccessibleInterval< VolatileAnnotationType< I > > getSource( final int t, final int level )
    {
        final RandomAccessibleInterval< V > rai = source.getSource( t, level );
        final RandomAccessibleInterval< VolatileAnnotationType< I > > convert = Converters.convert( rai, ( input, output ) -> {
            set( input, t, output );
        }, createVariable() );

        return convert;
    }

    @Override
    public RealRandomAccessible< VolatileAnnotationType< I > > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< V > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra, ( input, output ) -> set( input, t, output ), createVariable() );
    }

    private void set( V input, int t, VolatileAnnotationType< I > output )
    {
        if ( ! input.isValid() )
        {
            output.setValid( false );
            return;
        }

        final double label = input.get().getRealDouble();
        final I segment = adapter.getSegment( label, t, source.getName() );
        final VolatileAnnotationType< I > volatileAnnotationType = new VolatileAnnotationType( segment, true );
        output.set( volatileAnnotationType );
    }

    @Override
    public VolatileAnnotationType< I > getType()
    {
        return createVariable();
    }

    private VolatileAnnotationType< I > createVariable()
    {
        return new VolatileAnnotationType( adapter.createVariable(), true );
    }
}
