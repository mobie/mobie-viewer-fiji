package org.embl.mobie.viewer.transform;

import net.imglib2.roi.RealMaskRealInterval;

public interface RealMaskSource
{
	RealMaskRealInterval getRealMask();
}
