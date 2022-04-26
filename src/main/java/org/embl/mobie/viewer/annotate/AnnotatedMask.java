package org.embl.mobie.viewer.annotate;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.RealMaskRealInterval;

public interface AnnotatedMask
{
	RealMaskRealInterval mask();
	Integer timePoint();
	String name();
}
