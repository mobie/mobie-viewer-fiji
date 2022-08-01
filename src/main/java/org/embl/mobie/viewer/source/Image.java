package org.embl.mobie.viewer.source;

import net.imglib2.RealInterval;

public interface Image< T >
{
	SourcePair< T > getSourcePair();
	String getName();
	RealInterval getBounds(); // maybe make a mask
}
