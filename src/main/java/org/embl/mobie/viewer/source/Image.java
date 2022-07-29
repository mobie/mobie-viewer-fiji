package org.embl.mobie.viewer.source;

public interface Image< T >
{
	SourcePair< T > getSourcePair();
	String getName();
}
