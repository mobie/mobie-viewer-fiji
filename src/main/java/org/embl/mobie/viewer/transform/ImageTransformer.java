package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.transform.image.AffineTransformedImage;

public class ImageTransformer
{
	// Note that the Image Type may change, e.g. if this is an annotated label image
	// TODO is there a cleaner solution? Because for normal images the type would not change.
	public static Image< ? > transform( Image< ? > image, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		if ( image instanceof AnnotatedLabelImage )
		{
			return transformAnnotatedLabelImage( ( AnnotatedLabelImage ) image, affineTransform3D,  transformedImageName );
		}

		final AffineTransformedImage< ? > affineTransformedImage = new AffineTransformedImage<>( image, transformedImageName, affineTransform3D );

		return affineTransformedImage;
	}

	private static < A extends Annotation, TA extends A > DefaultAnnotatedLabelImage< TA > transformAnnotatedLabelImage( AnnotatedLabelImage< A > annotatedLabelImage, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		final Image< ? extends IntegerType< ? > > labelImage = annotatedLabelImage.getLabelImage();

		final AnnData< A > annData = annotatedLabelImage.getAnnData();

		final AnnotationAffineTransformer< A, TA > affineTransformer = new AnnotationAffineTransformer<>( affineTransform3D );

		TransformedAnnData< A, TA > transformedAnnData = new TransformedAnnData<>( annData, affineTransformer );

		final Image< ? extends IntegerType< ? > > transformedLabelImage = ( Image< ? extends IntegerType< ? > > ) transform( labelImage, affineTransform3D, transformedImageName );

		final DefaultAnnotatedLabelImage< TA > transformedAnnotatedImage = new DefaultAnnotatedLabelImage< TA >( transformedLabelImage, transformedAnnData );

		return transformedAnnotatedImage;
	}
}
