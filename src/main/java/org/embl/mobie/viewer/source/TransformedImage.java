package org.embl.mobie.viewer.source;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.transform.image.AffineTransformation;
import org.embl.mobie.viewer.transform.image.ImageTransformation;
import org.embl.mobie.viewer.transform.image.Transformation;

public class TransformedImage< A, B > implements Image< B >
{
	private final Image< A > image;
	private final ImageTransformation transformation;
	private Image< B > transformedImage;

	public TransformedImage( Image< A > image, ImageTransformation< A, B > transformation )
	{
		this.image = image;
		this.transformation = transformation;
	}

	private Image< B > getTransformedImage()
	{
		if ( transformedImage == null )
			transformedImage = transformation.apply( image );
		return transformedImage;
	}

	@Override
	public SourcePair< B > getSourcePair()
	{
		return getTransformedImage().getSourcePair();
	}

	@Override
	public String getName()
	{
		return transformation.getTransformedName( image );
	}

	public Transformation getTransformation()
	{
		return transformation;
	}

	public Image< A > getWrappedImage()
	{
		return image;
	}

	@Override
	public RealInterval getBounds( int t )
	{
		if ( transformedImage == null && transformation instanceof AffineTransformation )
		{
			// Compute the bounds without
			// creating the transformed image.
			// This can be advantageous for performance reasons
			// as it may avoid loading the image data.
			final RealInterval bounds = image.getBounds( t );
			final AffineTransform3D affineTransform3D = ( ( AffineTransformation< ? > ) transformation ).getAffineTransform3D();
			return affineTransform3D.estimateBounds( bounds );
		}

		return getTransformedImage().getBounds( t );
	}
}
