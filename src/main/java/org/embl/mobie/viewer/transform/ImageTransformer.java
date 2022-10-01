package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Segment;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.transform.image.AffineTransformedImage;

public class ImageTransformer
{
	private final Image< ? > image;

	public ImageTransformer( Image< ? > image )
	{
		this.image = image;
	}

	public Image< ? > getTransformedImage( AffineTransform3D affineTransform3D, String transformedImageName )
	{
		if ( image instanceof AnnotatedLabelImage )
		{
			// Transform the label image
			final AnnotatedLabelImage annotatedLabelImage = ( AnnotatedLabelImage ) image;
			final AffineTransformedImage< ? > transformedLabelImage = new AffineTransformedImage( image, transformedImageName, affineTransform3D );

			// Transform the annotations
			TransformedAnnData transformedAnnData;
			final Object annotation = annotatedLabelImage.getAnnData().getTable().annotations().iterator().next();
			if ( annotation instanceof Segment )
			{
				final AnnData< ? extends AnnotatedSegment > annData = annotatedLabelImage.getAnnData();
				final AnnotationTransformer annotationTransformer = new AnnotatedSegmentAffineTransformer( affineTransform3D );
				transformedAnnData = new TransformedAnnData( annData, annotationTransformer );
			} else
			{
				throw new UnsupportedOperationException( "Transformation of " + annotation.getClass().getName() + " is currently not supported" );
			}

			final AnnotatedLabelImage< ? extends AnnotatedSegment > transformedAnnotatedLabelImage = new AnnotatedLabelImage( transformedLabelImage, transformedAnnData );
			return transformedAnnotatedLabelImage;
		}
		else
		{
			final AffineTransformedImage< ? > affineTransformedImage = new AffineTransformedImage( image, transformedImageName, affineTransform3D );
			return affineTransformedImage;
		}
	}
}
