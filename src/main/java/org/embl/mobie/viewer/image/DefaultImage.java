package org.embl.mobie.viewer.image;

import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.source.DefaultSourcePair;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.source.SourcePair;

public class DefaultImage< T > implements Image< T >
{
	private final Source< T > source;
	private final Source< ? extends Volatile< T > > volatileSource;
	private final String name;

	public DefaultImage( Source< T > source, Source< ? extends Volatile< T > > volatileSource, String name )
	{
		this.source = source;
		this.volatileSource = volatileSource;
		this.name = name;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		return new DefaultSourcePair<>( source, volatileSource );
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return SourceHelper.estimateMask( source, 0 );
	}
}
