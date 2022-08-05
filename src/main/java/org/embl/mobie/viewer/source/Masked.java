package org.embl.mobie.viewer.source;

import net.imglib2.roi.RealMaskRealInterval;

public interface Masked
{
	// TODO: think about how to deal with time points
	RealMaskRealInterval getMask();
}
