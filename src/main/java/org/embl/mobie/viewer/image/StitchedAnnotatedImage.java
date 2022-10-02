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
package org.embl.mobie.viewer.image;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.VolatileAnnotationType;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.table.AnnDataHelper;

import javax.annotation.Nullable;
import java.util.List;

public class StitchedAnnotatedImage< A extends Annotation > extends StitchedImage< AnnotationType< A >, VolatileAnnotationType< A > > implements AnnotatedImage< A >
{
	private AnnData< A > annData;

	public StitchedAnnotatedImage( List< ? extends AnnotatedImage< A > > annotatedImages, Image< AnnotationType< A > > metadataImage, @Nullable List< int[] > positions, String imageName, double relativeCellMargin, boolean transformImages )
	{
		super( annotatedImages, metadataImage, positions, imageName, relativeCellMargin, transformImages );
		final List< ? extends Image< ? > > tileImages = getTileImages();
		annData = AnnDataHelper.getConcatenatedAnnData( annotatedImages );
	}

	@Override
	public AnnData< A > getAnnData()
	{
		return annData;
	}
}
