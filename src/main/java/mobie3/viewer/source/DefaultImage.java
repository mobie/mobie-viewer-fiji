package mobie3.viewer.source;

import bdv.viewer.Source;
import net.imglib2.Volatile;

public class DefaultImage< T > implements Image< T >
{
	private final Source< T > s;
	private final Source< ? extends Volatile< T > > vs;
	private final String name;

	public DefaultImage( Source< T > s, Source< ? extends Volatile< T > > vs, String name )
	{
		this.s = s;
		this.vs = vs;
		this.name = name;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		return new DefaultSourcePair<>( s, vs );
	}

	@Override
	public String getName()
	{
		return name;
	}
}
