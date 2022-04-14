package org.embl.mobie.viewer.annotate;

import net.imglib2.RealInterval;
import net.imglib2.roi.RealMask;

public interface AnnotatedMask
{
	RealMask getMask();
	Integer getTimepoint();
	String getName();
}
