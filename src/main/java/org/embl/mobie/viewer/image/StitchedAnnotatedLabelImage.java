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

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.VolatileAnnotationType;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.table.AnnDataHelper;

import javax.annotation.Nullable;
import java.util.List;

public class StitchedAnnotatedLabelImage< A extends Annotation > extends StitchedImage< AnnotationType< A >, VolatileAnnotationType< A > > implements AnnotatedLabelImage< A >
{
	private AnnData< A > annData;

	public StitchedAnnotatedLabelImage( List< ? extends AnnotatedLabelImage< A > > annotatedImages, Image< AnnotationType< A > > metadataImage, @Nullable List< int[] > positions, String imageName, double relativeCellMargin )
	{
		// FIXME
		//   One could just stitch the label mask images
		//   and then transform
		super( annotatedImages, metadataImage, positions, imageName, relativeCellMargin );
	}

	@Override
	public AnnData< A > getAnnData()
	{
		if ( annData == null )
			annData = AnnDataHelper.getConcatenatedAnnData( ( List ) getTranslatedImages() );

		return annData;
	}

	@Override
	public Image< ? extends IntegerType< ? > > getLabelImage()
	{
		// FIXME
		throw new RuntimeException();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		annData.getTable().transform( affineTransform3D );
		super.transform( affineTransform3D );
	}
}
