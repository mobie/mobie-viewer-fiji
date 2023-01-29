package org.embl.mobie.lib.color.opacity;

import net.imglib2.type.numeric.ARGBType;

import static net.imglib2.type.numeric.ARGBType.alpha;
import static net.imglib2.type.numeric.ARGBType.blue;
import static net.imglib2.type.numeric.ARGBType.green;
import static net.imglib2.type.numeric.ARGBType.red;

public interface OpacityAdjuster
{
	void setOpacity( double opacity );

	double getOpacity();

	default void adjustOpacity( ARGBType color, double opacity )
	{
		final int value = color.get();
		final int alpha = alpha( value );
		final double adjustedAlpha = alpha * opacity;
		color.set( ARGBType.rgba( red( value ), green( value ), blue( value ), adjustedAlpha ) );
	}
}
