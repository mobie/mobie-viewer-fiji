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

import bdv.viewer.Source;
import net.imglib2.*;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.Type;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.function.BiConsumer;

public class VolatileMaskedSource< T extends Type< T >, V extends Volatile< T > & Type< V > > extends AbstractMaskedSource< V >
{
    private final V type;

    public VolatileMaskedSource( Source< V > source, String name, RealMaskRealInterval mask )
    {
        super( source, name, mask );
        type = source.getType();
    }

    @Override
    protected RealRandomAccessible< V > createMaskedRealRandomAccessible(
            RealRandomAccessible< V > rra,
            RealMaskRealInterval mask )
    {
        return new FunctionRealRandomAccessible<>(
                3,
                new VolatileRealRandomAccessValueProvider( rra.realRandomAccess(), mask ),
                () -> type.createVariable() );
    }

    class VolatileRealRandomAccessValueProvider implements BiConsumer< RealLocalizable, V >
    {
        private final RealRandomAccess< V > ra;
        private final RealMaskRealInterval mask;

        public VolatileRealRandomAccessValueProvider( RealRandomAccess< V > ra, RealMaskRealInterval mask )
        {
            this.ra = ra;
            this.mask = mask;
        }

        @Override
        public void accept( RealLocalizable l, V output )
        {
            if ( ! mask.test( l ) )
            {
                // assumes that the default variable is the background value
                output.set( type.createVariable() );
                output.setValid( true );
                return;
            }

            final V input = ra.setPositionAndGet( l );
            if ( input.isValid() )
            {
                output.set( input.copy() );
            }
            else
            {
                output.setValid( false );
            }
        }
    }

    @Override
    protected RandomAccessibleInterval< V > createMaskedRandomAccessibleInterval( RandomAccessibleInterval< V > rai, RealMaskRealInterval mask )
    {
        FunctionRandomAccessible< V > fra =
                new FunctionRandomAccessible<>(
                        3,
                        new VolatileRandomAccessValueProvider( rai.randomAccess(), mask ),
                        () -> type.createVariable() );
        IntervalView< V > out = Views.interval( fra, rai );
        return out;
    }

    class VolatileRandomAccessValueProvider implements BiConsumer< Localizable, V >
    {
        private final RandomAccess< V > ra;
        private final RealMaskRealInterval mask;

        public VolatileRandomAccessValueProvider( RandomAccess< V > ra, RealMaskRealInterval mask )
        {
            this.ra = ra;
            this.mask = mask;
        }

        @Override
        public void accept( Localizable l, V output )
        {
            if ( ! mask.test( l ) )
            {
                // assumes that the default variable is the background value
                output.set( type.createVariable() );
                output.setValid( true );
                return;
            }

            final V input = ra.setPositionAndGet( l );
            if ( input.isValid() )
            {
                output.set( input.copy() );
            }
            else
            {
                output.setValid( false );
            }
        }
    }

}
