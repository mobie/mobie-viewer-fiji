package org.embl.mobie.viewer.image;

import org.embl.mobie.viewer.source.Masked;
import org.embl.mobie.viewer.source.SourcePair;

public interface Image< T > extends Masked
{
	SourcePair< T > getSourcePair();

	String getName();
}
