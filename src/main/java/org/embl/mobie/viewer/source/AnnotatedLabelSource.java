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
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.IntegerType;

public class AnnotatedLabelSource< T extends IntegerType< T >, A extends Annotation > extends AbstractSourceWrapper< T, AnnotationType< A > >
{
    private final AnnotationAdapter< A > annotationAdapter;

    public AnnotatedLabelSource( final Source< T > labelSource, AnnotationAdapter<A> annotationAdapter )
    {
        super( labelSource );
        this.annotationAdapter = annotationAdapter;
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< AnnotationType< A > > getSource( final int t, final int level )
    {
        return Converters.convert( source.getSource( t, level ), ( input, output ) -> {
            set( input, t, output );
        }, new AnnotationType( annotationAdapter.createVariable() ) );
    }

    @Override
    public RealRandomAccessible< AnnotationType< A > > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra,
                ( T input, AnnotationType< A > output ) ->
                set( input, t, output ),
                new AnnotationType<>() );
    }

    private void set( T input, int t, AnnotationType< A > output  )
    {
        final int label = input.getInteger();

        final A annotation = annotationAdapter.getAnnotation( getName(), t, label );
        output.setAnnotation( annotation );
    }

    @Override
    public AnnotationType< A > getType()
    {
        return new AnnotationType( annotationAdapter.createVariable() );
    }
}
