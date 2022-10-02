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
package org.embl.mobie.viewer.transform.image;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.Segment;
import org.embl.mobie.viewer.image.AnnotatedImage;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.transform.AnnotatedSegmentAffineTransformer;
import org.embl.mobie.viewer.transform.AnnotationAffineTransformer;
import org.embl.mobie.viewer.transform.AnnotationTransformer;
import org.embl.mobie.viewer.transform.TransformedAnnData;

public class AffineTransformedAnnotatedImage< A extends Annotation > extends AffineTransformedImage< AnnotationType< A > > implements AnnotatedImage< A >
{
	private final AnnotatedImage< A > annotatedImage;
	private AnnData< A > transformedAnnData;

	public AffineTransformedAnnotatedImage( AnnotatedImage< A > image, String transformedImageName, AffineTransform3D affineTransform3D )
	{
		super( image, transformedImageName, affineTransform3D );
		this.annotatedImage = image;
	}

	@Override
	public AnnData< A > getAnnData()
	{
		if ( transformedAnnData == null )
		{
			final AnnData< A > annData = annotatedImage.getAnnData();

			final AnnotationAffineTransformer affineTransformer = new AnnotationAffineTransformer( affineTransform3D );
			transformedAnnData = new TransformedAnnData( annData, affineTransformer );

			// TODO: This will cause loading of the data.
			//   This is an an issue.
//			final A annotation = annData.getTable().annotations().iterator().next();
//			if ( annotation instanceof Segment )
//			{
//				final AnnData< ? extends AnnotatedSegment > segmentAnnData = ( AnnData< ? extends AnnotatedSegment > ) annotatedImage.getAnnData();
//				final AnnotationTransformer annotationTransformer = new AnnotatedSegmentAffineTransformer( affineTransform3D );
//
//				transformedAnnData = new TransformedAnnData( segmentAnnData, annotationTransformer );
//			}
//			else
//			{
//				throw new UnsupportedOperationException( "Transformation of " + annotation.getClass().getName() + " is currently not supported" );
//			}
		}

		return transformedAnnData;
	}
}
