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
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.Type;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BiConsumer;

public class BoundarySource< T extends Type< T > > extends AbstractBoundarySource< T >
{
    public BoundarySource( Source< T > source, boolean showAsBoundaries, @Nullable float boundaryWidth, @Nullable RealInterval bounds )
    {
        super( source, showAsBoundaries, boundaryWidth, bounds );
    }

    @Override
    protected RealRandomAccessible< T > createBoundaries( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, double[] pixelUnitsBoundaryWidth )
    {
        BiConsumer< RealLocalizable, T > biConsumer = ( l, output ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            final T pixelValue = access.setPositionAndGet( l ).copy();
            // assumes that the default variable is the background value
            final T background = pixelValue.createVariable();

            // set to background
            output.set( background );

            if ( pixelValue.valueEquals( background ) )
                return;

            // check whether input is a boundary pixel
            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // back and forth
                {
                    access.move( signum * pixelUnitsBoundaryWidth[ d ], d );
                    final T neighbourValue = access.get();
                    if ( ! neighbourValue.valueEquals( pixelValue )  )
                    {
                        // input is a non-background boundary pixel...
                        // ...thus it keeps its value
                        output.set( pixelValue );
                        return;
                    }
                    // move back to center
                    access.move( - signum * pixelUnitsBoundaryWidth[ d ], d );
                }
            }
        };

        return new FunctionRealRandomAccessible( 3, biConsumer, () -> getType().createVariable() );
    }
}
