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

public class MaskedSource< T extends Type< T > > extends AbstractMaskedSource< T >
{
    private final T type;

    public MaskedSource( Source<T> source, String name, RealMaskRealInterval mask )
    {
        super( source, name, mask );
        type = source.getType();
    }

    @Override
    protected RealRandomAccessible< T > createMaskedRealRandomAccessible(
            RealRandomAccessible< T > rra,
            RealMaskRealInterval mask )
    {
        return new FunctionRealRandomAccessible< T >(
                3,
                new RealRandomAccessValueProvider( rra.realRandomAccess(), mask ),
                type::createVariable );
    }

    class RealRandomAccessValueProvider implements BiConsumer< RealLocalizable, T >
    {
        private final RealRandomAccess< T > ra;
        private final RealMaskRealInterval mask;

        public RealRandomAccessValueProvider( RealRandomAccess< T > ra, RealMaskRealInterval mask )
        {
            this.ra = ra;
            this.mask = mask;
        }

        @Override
        public void accept( RealLocalizable l, T output )
        {
            if ( ! mask.test( l ) )
            {
                output.set(  type.createVariable() );
            }
            else
            {
                output.set( ra.setPositionAndGet( l ).copy() );
            }
        }
    }

    @Override
    protected RandomAccessibleInterval< T > createMaskedRandomAccessibleInterval(
            RandomAccessibleInterval< T > rai,
            RealMaskRealInterval mask )
    {
        FunctionRandomAccessible< T > fra = new FunctionRandomAccessible<>(
                3,
                new RandomAccessValueProvider( rai.randomAccess(), mask ),
                type::createVariable );

        return Views.interval( fra, rai );
    }

    class RandomAccessValueProvider implements BiConsumer< Localizable, T >
    {
        private final RandomAccess< T > ra;
        private final RealMaskRealInterval mask;

        public RandomAccessValueProvider( RandomAccess< T > ra, RealMaskRealInterval mask )
        {
            this.ra = ra;
            this.mask = mask;
        }

        @Override
        public void accept( Localizable l, T output )
        {
            if ( ! mask.test( l ) )
            {
                output.set(  type.createVariable() );
            }
            else
            {
                output.set( ra.setPositionAndGet( l ).copy() );
            }
        }
    }

}
