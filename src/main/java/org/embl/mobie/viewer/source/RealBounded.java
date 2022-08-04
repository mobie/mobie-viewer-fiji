package org.embl.mobie.viewer.source;

import net.imglib2.roi.RealMaskRealInterval;

public interface RealBounded
{
	RealMaskRealInterval getBounds( int t );
}
