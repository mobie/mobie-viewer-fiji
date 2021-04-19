package de.embl.cba.mobie2.grid;

import net.imglib2.Interval;
import net.imglib2.RealInterval;

public interface AnnotatedInterval
{
	RealInterval getInterval();
	String getName();
}
