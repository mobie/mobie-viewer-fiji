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
package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.NumericType;

public class SourceCropper< T extends NumericType<T> > {

    private final SourceAndConverter< T > sourceAndConverter;
    private final String name;
    private final RealInterval realInterval;
    private final boolean zeroMin;

    public SourceCropper( SourceAndConverter< T > sourceAndConverter, String name, RealInterval realInterval, boolean zeroMin )
    {
        this.sourceAndConverter = sourceAndConverter;
        this.name = name;
        this.realInterval = realInterval;
        this.zeroMin = zeroMin;
    }

    public SourceAndConverter< T > get()
    {
        CroppedSource< T > src = new CroppedSource<>( sourceAndConverter.getSpimSource(), name, realInterval, zeroMin );

        if ( sourceAndConverter.asVolatile() != null )
        {
            CroppedSource< ? extends Volatile< T > > vsrc = new CroppedSource( sourceAndConverter.asVolatile().getSpimSource(), name, realInterval, zeroMin );

//            SourceAndConverter< ? extends Volatile< T > > vsac = new SourceAndConverter( vsrc, sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile() );

            SourceAndConverter< ? extends Volatile< T > > vsac = new SourceAndConverter( vsrc, sourceAndConverter.asVolatile().getConverter() );

            final SourceAndConverter< T > outputSourceAndConverter = new SourceAndConverter<>( src, sourceAndConverter.getConverter(), vsac );

            return outputSourceAndConverter;
        }
        else
        {
            return new SourceAndConverter< T >( src, sourceAndConverter.getConverter() );
        }
    }
}
