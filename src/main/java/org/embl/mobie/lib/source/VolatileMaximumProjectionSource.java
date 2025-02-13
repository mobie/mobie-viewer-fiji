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

import bdv.viewer.Source;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

import java.util.function.BiConsumer;

public class VolatileMaximumProjectionSource< T extends NumericType< T >, V extends Volatile< T > & Type< V > >
    extends AbstractMaximumProjectionSource< V >
{
    public VolatileMaximumProjectionSource( Source< V > source, boolean doProjection, float depth )
    {
        super( source, doProjection, depth);
    }

    @Override
    protected RealRandomAccessible< V > createMaximumProjection( RealRandomAccessible< V > rra, int depth )
    {
        BiConsumer<RealLocalizable, V> boundaries = (l, output) -> {

            final RealRandomAccess< V > access = rra.realRandomAccess();
            access.setPosition( l );
            access.move( - depth / 2 , 2);

            boolean allValid = true;
            V maxVal = access.get().copy();
            if ( ! maxVal.isValid() ) {
                output.setValid(false);
                return;
            }

            double maxValue = ((RealType<?>) maxVal).getRealDouble();

            for ( int i = 1; i < depth; i++ ) {
                access.move(1, 2);
                V currentValue = access.get();
                if ( ! currentValue.isValid() ) {
                    output.setValid(false);
                    return;
                }
                double currentRealDouble = ((RealType<?>) currentValue).getRealDouble();
                if ( currentRealDouble > maxValue ) {
                    maxValue = currentRealDouble;
                    maxVal.set( currentValue.copy() );
                }
            }

            output.set( maxVal );
            output.setValid( true );
        };

        final V type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< V > randomAccessible =
                new FunctionRealRandomAccessible( 3, boundaries, () -> type.createVariable() );
        return randomAccessible;
    }
}
