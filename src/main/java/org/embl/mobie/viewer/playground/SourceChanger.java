/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package org.embl.mobie.viewer.playground;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

import java.util.function.Function;

public class SourceChanger< T > implements Function< SourceAndConverter< ? >,  SourceAndConverter< ? > >
{
    private final Function< Source< ? >, Source< ? > > sourceConverter;

    public SourceChanger( Function< Source< ? >, Source< ? > > sourceConverter )
    {
        this.sourceConverter = sourceConverter;
    }

    @Override
    public SourceAndConverter< ? > apply( SourceAndConverter< ? > inputSourceAndConverter )
    {
        Source< ? > outputSource = sourceConverter.apply( inputSourceAndConverter.getSpimSource() );

        if ( inputSourceAndConverter.asVolatile() != null )
        {
            final Source< ? > volatileOutputSource = sourceConverter.apply( inputSourceAndConverter.asVolatile().getSpimSource() );

            final SourceAndConverter< ? > volatileOutputSourceAndConverter = new SourceAndConverter( volatileOutputSource, inputSourceAndConverter.asVolatile().getConverter() );

            final SourceAndConverter< ? > outputSourceAndConverter = new SourceAndConverter( outputSource, inputSourceAndConverter.getConverter(), volatileOutputSourceAndConverter );

            return outputSourceAndConverter;
        }
        else
        {
            return new SourceAndConverter( outputSource, inputSourceAndConverter.getConverter() );
        }
    }
}
