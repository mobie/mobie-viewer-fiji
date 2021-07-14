package de.embl.cba.mobie.annotate;

import net.imglib2.RealInterval;

public interface AnnotatedInterval
{
	RealInterval getInterval();
	Integer getTimepoint();
	String getName();
}
