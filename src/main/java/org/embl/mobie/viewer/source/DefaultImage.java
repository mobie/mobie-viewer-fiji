package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;

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
	public RealInterval getBounds( int t )
	{
		return SourceHelper.estimateBounds( source );
	}
}
