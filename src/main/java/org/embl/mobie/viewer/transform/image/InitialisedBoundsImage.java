package org.embl.mobie.viewer.transform.image;

import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SourcePair;

public class InitialisedBoundsImage< T > implements Image< T >
{
	private final Image< T > image;
	private final RealMaskRealInterval mask;

	public InitialisedBoundsImage( Image< T > image, RealMaskRealInterval mask )
	{
		this.image = image;
		this.mask = mask;
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
	public RealMaskRealInterval getMask( )
	{
		return mask;
	}
}
