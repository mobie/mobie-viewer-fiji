package org.embl.mobie.viewer.source;

import net.imglib2.RealInterval;
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

	@Override
	public RealInterval getBounds()
	{
		return getTransformedImage().getBounds();
	}

	public Transformation getTransformation()
	{
		return transformation;
	}

	public Image< A > getWrappedImage()
	{
		return image;
	}
}
