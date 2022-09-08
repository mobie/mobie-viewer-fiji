package org.embl.mobie.viewer.source;

import net.imglib2.roi.RealMaskRealInterval;

public interface Masked
{
	// TODO:
	//  - think about how to deal with time points
	//  - think about how to forward that to the sources.
	//    right now only images now about this
	//    where I create the SAC I could wrap the sources into a
	//    MaskAwareSource.
	RealMaskRealInterval getMask();
}
