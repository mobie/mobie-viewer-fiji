package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import net.imglib2.Volatile;

public class DefaultSourcePair< T > implements SourcePair< T >
{
	private final Source< T > s;
	private final Source< ? extends Volatile< T > > vs;

	public DefaultSourcePair( Source< T > s, Source< ? extends Volatile< T > > vs )
	{
		this.s = s;
		this.vs = vs;
	}

	@Override
	public Source< T > getSource()
	{
		return s;
	}

	@Override
	public Source< ? extends Volatile< T > > getVolatileSource()
	{
		return vs;
	}
}
