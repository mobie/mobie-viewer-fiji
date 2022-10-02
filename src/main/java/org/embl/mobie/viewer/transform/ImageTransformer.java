package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Segment;
import org.embl.mobie.viewer.image.AnnotatedImage;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.transform.image.AffineTransformedAnnotatedImage;
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
		if ( image instanceof AnnotatedImage )
		{
			final AffineTransformedAnnotatedImage< ? > affineTransformedAnnotatedImage = new AffineTransformedAnnotatedImage( ( AnnotatedImage ) image, transformedImageName, affineTransform3D );

			return affineTransformedAnnotatedImage;
		}
		else
		{
			final AffineTransformedImage< ? > affineTransformedImage = new AffineTransformedImage( image, transformedImageName, affineTransform3D );
			return affineTransformedImage;
		}
	}
}
