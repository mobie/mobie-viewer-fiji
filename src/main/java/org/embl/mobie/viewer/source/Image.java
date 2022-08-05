package org.embl.mobie.viewer.source;

public interface Image< T > extends Masked
{
	SourcePair< T > getSourcePair();
	String getName();
}
