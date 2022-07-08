package mobie3.viewer.source;

import bdv.viewer.Source;
import net.imglib2.Volatile;

public interface SourcePair< T >
{
	Source< T > getSource();
	Source< ? extends Volatile< T > > getVolatileSource();
}
