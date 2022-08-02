package org.embl.mobie.viewer.source;

import net.imglib2.RealInterval;

public interface RealBounded
{
	RealInterval getBounds( int t );
}
