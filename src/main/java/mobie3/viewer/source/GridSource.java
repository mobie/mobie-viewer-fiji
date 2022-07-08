package mobie3.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

public interface GridSource
{
	static boolean instanceOf( SourceAndConverter< ? > sourceAndConverter )
	{
		final Source< ? > source = sourceAndConverter.getSpimSource();
		return instanceOf( source );
	}

	static boolean instanceOf( Source< ? > source )
	{
		if ( source instanceof SourceWrapper )
		{
			source = ( ( SourceWrapper ) source ).getWrappedSource();
			return GridSource.instanceOf( source );
		}
		else
		{
			return source instanceof GridSource;
		}
	}
}
