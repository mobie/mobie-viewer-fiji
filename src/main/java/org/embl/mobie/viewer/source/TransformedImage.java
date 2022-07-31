package org.embl.mobie.viewer.source;

import org.embl.mobie.viewer.transform.image.ImageTransformation;
import org.embl.mobie.viewer.transform.image.Transformation;

public class TransformedImage< A, B > implements Image< B >
{
	private final Image< A > image;
	private final Transformation transformation;

	private Image< B > transformedImage;

	public TransformedImage( Image< A > image, ImageTransformation< A, B > transformation )
	{
		this.image = image;
		this.transformation = transformation;

		// If below call turns out to access
		// data on disk, one can make this lazy and
		// only call this once getSourcePair() is called.
		// For getName() one has to think...maybe it would
		// be necessary to add a getName( image ) method
		// to the Transformation.
		transformedImage = transformation.apply( image );
	}

	@Override
	public SourcePair< B > getSourcePair()
	{
		return transformedImage.getSourcePair();
	}

	@Override
	public String getName()
	{
		return transformedImage.getName();
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
