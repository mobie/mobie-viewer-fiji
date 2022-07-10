package mobie3.viewer.source;

import mobie3.viewer.transform.ImageTransformer;

public class TransformedImage< T > implements Image< T >
{
	private final Image image;
	private final ImageTransformer transformer;

	private Image< T > transformedImage;

	public TransformedImage( Image< T > image, ImageTransformer transformer )
	{
		this.image = image;
		this.transformer = transformer;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		if ( transformedImage == null )
		{
			transformedImage = transformer.transform( image );
		}

		return transformedImage.getSourcePair();
	}

	@Override
	public String getName()
	{
		return transformedImage.getName();
	}
}
