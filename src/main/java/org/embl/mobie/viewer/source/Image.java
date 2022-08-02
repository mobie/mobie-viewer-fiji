package org.embl.mobie.viewer.source;


public interface Image< T > extends RealBounded
{
	SourcePair< T > getSourcePair();
	String getName();
}
