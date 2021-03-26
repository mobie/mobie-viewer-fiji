package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;

import java.util.ArrayList;
import java.util.List;

public class ImageDisplay extends SourceDisplay
{
	private final String color;
	private final double[] contrastLimits;

	// For serialisation
	public ImageDisplay( String name, List< String > sources, String color, double[] contrastLimits )
	{
		super( name, sources );
		this.color = color;
		this.contrastLimits = contrastLimits;
	}

	public ImageDisplay createSerializableCopy()
	{
		final ArrayList< String > sources = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			sources.add( sourceAndConverter.getSpimSource().getName() );
		}
		// TODO
		//   - fetch the rest and construct an ImageDisplay

		return null;
	}

	public String getColor()
	{
		return color;
	}

	public double[] getContrastLimits()
	{
		return contrastLimits;
	}
}
