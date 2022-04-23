package org.embl.mobie.viewer.annotate;

import net.imglib2.roi.RealMaskRealInterval;

public interface AnnotatedMask
{
	RealMaskRealInterval getMask();
	Integer getTimepoint();
	String getName();
}
