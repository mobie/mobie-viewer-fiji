package org.embl.mobie.lib.source;

import net.imglib2.roi.RealMaskRealInterval;

public interface Masked
{
	// TODO:
	//  - think about how to deal with time points
	//  - think about how to forward that to the sources.
	RealMaskRealInterval getMask();

	void setMask( RealMaskRealInterval mask );
}
