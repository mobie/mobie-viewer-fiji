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
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

public class BoundarySource< T extends Type< T > > extends AbstractBoundarySource< T >
{
    public BoundarySource( final Source< T > source )
    {
       super( source, null, null );
    }

    public BoundarySource( final Source< T > source, RealMaskRealInterval bounds, Collection< Integer > timePoints )
    {
        super( source, bounds, timePoints );
    }

    @Override
    protected RealRandomAccessible< T > createBoundaryImage( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {
        // assumes that the default variable is the background value
        final T background = getType().createVariable();

        BiConsumer< RealLocalizable, T > biConsumer = ( l, output ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            final T center = access.setPositionAndGet( l ).copy();


            // by default set to background...
            output.set( background );

            if ( center.valueEquals( background ) )
                return;

            // ...unless it is a boundary pixel
            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    if ( ! access.get().valueEquals( center )  )
                    {
                        // boundary pixel
                        output.set( center );
                        return;
                    }
                    // move back to center
                    access.move( - signum * boundaryWidth[ d ], d );
                }
            }
        };

        final FunctionRealRandomAccessible< T > boundaries = new FunctionRealRandomAccessible( 3, biConsumer, () -> background );
        return boundaries;
    }
}
