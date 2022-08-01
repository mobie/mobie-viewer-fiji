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

import bdv.viewer.Source;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class VolatileBoundarySource< T extends Type< T >, V extends Volatile< T > & Type< V > > extends AbstractBoundarySource< V >
{
    public VolatileBoundarySource( final Source< V > source, @Nullable RealInterval bounds )
    {
        super( source, bounds );
    }

    @Override
    protected RealRandomAccessible< V > createBoundaryImage( RealRandomAccessible< V > rra, ArrayList< Integer > boundaryDimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, V > boundaries = ( l, output ) ->
        {
            final RealRandomAccess< V > access = rra.realRandomAccess();
            final V input = access.setPositionAndGet( l ).copy();
            if ( ! input.isValid() )
            {
                output.setValid( false );
                return;
            }

            // assumes that the default variable is the background value
            final V background = input.createVariable();

            // set to valid background...
            output.set( background );
            output.setValid( true );

            if ( input.valueEquals( background )  )
                return;

            // ...unless it is a boundary pixel
            for ( Integer d : boundaryDimensions )
            {
                for ( int signum = -1; signum <= +1; signum +=2  ) // back and forth
                {
                    access.move( signum * boundaryWidth[ d ], d );

                    if ( ! access.get().isValid() )
                    {
                        // a pixel around the input is not valid
                        // thus we cannot yet determine whether
                        // it is a boundary pixel
                        output.setValid( false );
                        return;
                    }

                    if ( ! access.get().valueEquals( input )  )
                    {
                        // a pixel around the input
                        // has a different value,
                        // thus the input is a boundary pixel,
                        // thus it keeps its value
                        output.set( input );
                        return;
                    }

                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
        };

        final V type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< V > randomAccessible = new FunctionRealRandomAccessible( 3, boundaries, () -> type.createVariable() );
        return randomAccessible;
    }
}
