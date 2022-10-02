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
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.AnnotationAdapter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.IntegerType;

import java.util.concurrent.atomic.AtomicBoolean;

// MAYBE: This does not need to know that this is a Segment?!
public class VolatileAnnotatedLabelSource< T extends IntegerType< T >, V extends Volatile< T >, A extends Annotation > extends AbstractSourceWrapper< V, VolatileAnnotationType< A > >
{
    private final AtomicBoolean throwError = new AtomicBoolean( true );

    private final AnnotationAdapter< A > annotationAdapter;

    public VolatileAnnotatedLabelSource( final Source< V > source, AnnotationAdapter< A > annotationAdapter )
    {
        super( source );
        this.annotationAdapter = annotationAdapter;
    }

    @Override
    public RandomAccessibleInterval< VolatileAnnotationType< A > > getSource( final int t, final int level )
    {
        final RandomAccessibleInterval< V > rai = source.getSource( t, level );
        final RandomAccessibleInterval< VolatileAnnotationType< A > > convert = Converters.convert( rai, ( input, output ) -> {
            set( input, t, output );
        }, createVariable() );

        return convert;
    }

    @Override
    public RealRandomAccessible< VolatileAnnotationType< A > > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< V > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra, ( input, output ) -> set( input, t, output ), createVariable() );
    }

    private void set( V input, int t, VolatileAnnotationType< A > output )
    {
        if ( ! input.isValid() )
        {
            output.setValid( false );
            return;
        }

        final int label = input.get().getInteger();

        if ( label == 0 )
        {
            // 0 is the background label
            // null is the background annotation
            output.get().setAnnotation( null );
            output.setValid( true );
            return;
        }

        final A annotation = annotationAdapter.getAnnotation( getName(), t, label );
        if ( annotation == null )
        {
            if ( throwError.get() )
            {
                // Note: this Exception is swallowed by BDV
                // This only serves to put a breakpoint for debugging.
                System.err.println( "Could not find annotation: " + getName() + "; time point = " + t + "; label = " + label );
                System.err.println( "Suppressing further errors of that kind.");
            }

            throwError.set( false ); // Not to crash the system by too many serr prints
        }
        output.get().setAnnotation( annotation );
        output.setValid( true );
    }

    @Override
    public VolatileAnnotationType< A > getType()
    {
        return createVariable();
    }

    private VolatileAnnotationType< A > createVariable()
    {
        return new VolatileAnnotationType( annotationAdapter.createVariable(), true );
    }
}
