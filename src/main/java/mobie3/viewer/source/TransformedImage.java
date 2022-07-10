package mobie3.viewer.source;

import mobie3.viewer.transform.Transformation;

public class TransformedImage< T > implements Image< T >
{
	private final Image< T > image;
	private final Transformation transformation;

	private Image< T > transformedImage;

	public TransformedImage( Image< T > image, Transformation transformation )
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
	public SourcePair< T > getSourcePair()
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

	public Image< T > getWrappedImage()
	{
		return image;
	}
}
