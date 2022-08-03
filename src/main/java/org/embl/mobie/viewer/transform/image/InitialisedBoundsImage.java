package org.embl.mobie.viewer.transform.image;

import net.imglib2.RealInterval;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SourcePair;

public class InitialisedBoundsImage< T > implements Image< T >
{
	private final Image< T > image;
	private final RealInterval bounds;

	public InitialisedBoundsImage( Image< T > image, RealInterval bounds )
	{
		this.image = image;
		this.bounds = bounds;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		return image.getSourcePair();
	}

	@Override
	public String getName()
	{
		return image.getName();
	}

	@Override
	public RealInterval getBounds( int t )
	{
		return bounds;
	}
}
