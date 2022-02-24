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
package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

public class SourceAndConverterCropper< T extends NumericType<T> >
{
    private final SourceAndConverter croppedSourceAndConverter;

    public SourceAndConverterCropper( SourceAndConverter< T > sourceAndConverter, String name, RealInterval maskInterval, AffineTransform3D maskTransform, boolean rectify, boolean center )
    {
        final MaskedSource maskedSource = new MaskedSource( sourceAndConverter.getSpimSource(), name, maskInterval, maskTransform, rectify, center );

        final MaskedSource volatileMaskedSource = new MaskedSource( sourceAndConverter.asVolatile().getSpimSource(), name, maskInterval, maskTransform, rectify, center );

        croppedSourceAndConverter = new SourceAndConverter( maskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.getConverter(), sourceAndConverter ), new SourceAndConverter( volatileMaskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile() ) ) );
    }

    public SourceAndConverter get()
    {
        return croppedSourceAndConverter;
    }
}
