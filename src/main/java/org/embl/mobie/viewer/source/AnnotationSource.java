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

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

public class AnnotationSource< A, T extends AnnotationType< A > > extends AbstractAnnotationSource< T >
{
    public AnnotationSource( final Source< T > source )
    {
       super( source, null, null );
    }

    public AnnotationSource( final Source< T > source, RealMaskRealInterval bounds, Collection< Integer > timePoints )
    {
        super( source, bounds, timePoints );
    }

    //@Override
    protected RealRandomAccessible< T > createBoundaryImageNew( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {
        //final AnnotationType< A > background = rra.realRandomAccess().get().createVariable();

        BiConsumer< RealLocalizable, T > biConsumer = ( l, output ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            final AnnotationType< A > center = access.setPositionAndGet( l ).copy();

            final A a1 = center.get();
            if ( a1 != null )
            {
                int b = 1;
            }

            if ( center.valueEquals( center.createVariable() ) )
            {
                if ( center.get() != null )
                {
                    int a = 1;
                }
                output.set( center.createVariable() );
                return; // background
            }

            final A centerAnnotation = center.get();

            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    final A annotation = access.get().get();
                    if ( ! ( centerAnnotation == annotation ) )
                    {
                        // boundary pixel
                        output.set( center.copy() );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            // no boundary pixel
            output.set( center.createVariable() );
            return;
        };

        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > boundaries = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.createVariable() );
        return boundaries;
    }


    protected RealRandomAccessible< T > createBoundaryImage( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {

        BiConsumer< RealLocalizable, T > biConsumer = ( l, output ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            final AnnotationType< A > center = access.setPositionAndGet( l ).copy();
            final A centerAnnotation = center.get();
            final AnnotationType< A > background = getType().createVariable();
            if ( centerAnnotation != null )
            {
                int a = 1;
            }

            if ( centerAnnotation == null )
            {
                try
                {
                    if ( center.valueEquals( background ) )
                    {
                        int b = 1;
                    }
                } catch ( Exception e )
                {
                    int c = 1;
                }
                output.set( center.createVariable() );
                return; // background
            }

            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    final A annotation = access.get().get();
                    if ( ! ( centerAnnotation == annotation ) )
                    {
                        // boundary pixel
                        output.set( center.copy() );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            // no boundary pixel
            output.set( center.createVariable() );
            return;
        };

        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > boundaries = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.createVariable() );
        return boundaries;
    }
}
