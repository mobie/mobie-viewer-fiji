package org.embl.mobie.viewer.annotate;

import net.imglib2.RealInterval;

public interface AnnotatedInterval
{
	RealInterval getInterval();
	Integer getTimepoint();
	String getName();
}
