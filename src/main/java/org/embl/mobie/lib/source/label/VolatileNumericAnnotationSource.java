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
package org.embl.mobie.lib.source.label;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.volatiles.VolatileDoubleType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.source.AbstractSourceWrapper;
import org.embl.mobie.lib.source.AnnotationType;

public class VolatileNumericAnnotationSource< A extends Annotation, V extends Volatile< AnnotationType< A > > > extends AbstractSourceWrapper< V, VolatileDoubleType >
{
    private final String featureName;

    public VolatileNumericAnnotationSource( Source< V > source, String featureName )
    {
        super( source );
        this.featureName = featureName;
    }

    @Override
    public boolean isPresent( final int t )
    {
        return source.isPresent( t );
    }

    @Override
    public RandomAccessibleInterval< VolatileDoubleType > getSource( final int t, final int level )
    {
        RandomAccessibleInterval< V > rai = source.getSource( t, level );

        return Converters.convert( rai,
                ( input, output ) ->
                { setOutput( input, t, output ); }, new VolatileDoubleType()  );
    }

    @Override
    public RealRandomAccessible< VolatileDoubleType > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        RealRandomAccessible< V > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        return Converters.convert( rra,
                ( input, output ) ->
                setOutput( input, t, output ),
                new VolatileDoubleType() );
    }

    private void setOutput( Volatile< AnnotationType< A > > input, int t, Volatile< DoubleType > output  )
    {
        if ( ! input.isValid() )
        {
            output.setValid( false );
            return;
        }

        A annotation = input.get().getAnnotation();

        if ( annotation == null )
        {
            // background
            output.get().setReal( 0 );
        }
        else
        {
            Double number = annotation.getNumber( featureName );
            output.get().setReal( number );
        }

        output.setValid( true );
    }

    @Override
    public VolatileDoubleType getType()
    {
        return new VolatileDoubleType();
    }
}
