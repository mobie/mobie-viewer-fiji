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
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		if ( transformedImage == null )
		{
			transformedImage = transformation.apply( image );
		}

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
