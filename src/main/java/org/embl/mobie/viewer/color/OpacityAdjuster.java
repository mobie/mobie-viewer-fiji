package org.embl.mobie.viewer.color;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

import static net.imglib2.type.numeric.ARGBType.alpha;
import static net.imglib2.type.numeric.ARGBType.blue;
import static net.imglib2.type.numeric.ARGBType.green;
import static net.imglib2.type.numeric.ARGBType.red;

public interface OpacityAdjuster
{

	void setOpacity( double opacity );
	double getOpacity();

	static void adjustOpacity( SourceAndConverter< ? > sourceAndConverter, double opacity )
	{
		final Converter< ?, ARGBType > converter = sourceAndConverter.getConverter();
		if ( converter instanceof OpacityAdjuster )
		{
			( ( OpacityAdjuster ) converter ).setOpacity( opacity );

			if ( sourceAndConverter.asVolatile() != null )
				( ( OpacityAdjuster ) sourceAndConverter.asVolatile().getConverter() ).setOpacity( opacity );
		}
	}

	static void adjustAlpha( ARGBType color, double opacity )
	{
		final int value = color.get();
		color.set( ARGBType.rgba( red( value ), green( value ), blue( value ), alpha( value ) * opacity ) );
	}

}
