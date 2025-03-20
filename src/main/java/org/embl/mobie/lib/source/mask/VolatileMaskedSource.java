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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class VolatileMaskedSource< T extends Type< T >, V extends Volatile< T > & Type< V > > extends AbstractMaskedSource< V >
{
    public VolatileMaskedSource( Source< V > source, String name, RealMaskRealInterval mask )
    {
        super( source, name, mask );
    }

    @Override
    protected RealRandomAccessible< V > createMaskedRealRandomAccessible(
            RealRandomAccessible< V > rra,
            RealMaskRealInterval mask )
    {
        BiConsumer< RealLocalizable, V > masked = ( l, output ) ->
        {
            final RealRandomAccess< V > access = rra.realRandomAccess();
            final V input = access.setPositionAndGet( l ).copy();

            if( ! mask.test( l ) )
            {
                // assumes that the default variable is the background value
                final V background = input.createVariable();
                output.set( background );
                output.setValid( true );
            }
            else if ( ! input.isValid() )
            {
                output.setValid( false );
            }
            else
            {
                output.set( input );
            }
        };

        V type = rra.getType();
        Supplier< V > vSupplier = () -> type.createVariable();
        return new FunctionRealRandomAccessible<>( 3, masked, vSupplier );
    }

    @Override
    protected RandomAccessibleInterval< V > createMaskedRandomAccessibleInterval( RandomAccessibleInterval< V > rai, RealMaskRealInterval mask )
    {
        BiConsumer< Localizable, V > masked = ( l, output ) ->
        {
            final RandomAccess< V > access = rai.randomAccess();
            final V input = access.setPositionAndGet( l ).copy();

            if( ! mask.test( l ) )
            {
                // assumes that the default variable is the background value
                final V background = input.createVariable();
                output.set( background );
                output.setValid( true );
            }
            else if ( ! input.isValid() )
            {
                output.setValid( false );
            }
            else
            {
                output.set( input );
            }
        };

        V type = rai.getType();
        Supplier< V > vSupplier = () -> type.createVariable();
        FunctionRandomAccessible< V > fra = new FunctionRandomAccessible<>( 3, masked, vSupplier );
        IntervalView< V > out = Views.interval( fra, rai );
        return out;
    }
}
