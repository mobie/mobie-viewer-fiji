package org.embl.mobie.viewer.color;

import bdv.viewer.SourceAndConverter;

public interface OpacityAdjuster
{
	void setOpacity( double opacity );
	double getOpacity();

	static void adjustOpacity( SourceAndConverter< ? > sourceAndConverter, double opacity )
	{
		try
		{
			( ( OpacityAdjuster ) sourceAndConverter.getConverter() ).setOpacity( opacity );

			if ( sourceAndConverter.asVolatile() != null )
				( ( OpacityAdjuster ) sourceAndConverter.asVolatile().getConverter() ).setOpacity( opacity );
		}
		catch ( Exception e )
		{
			throw new RuntimeException("Cannot adjust the opacity of " + sourceAndConverter.getConverter() );
		}
	}

}
