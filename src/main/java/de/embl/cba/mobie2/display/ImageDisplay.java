package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;

import java.util.ArrayList;
import java.util.List;

public class ImageDisplay extends SourceDisplay
{
	private final String color;
	private final double[] contrastLimits;
	private final BlendingMode blendingMode;

	// For serialisation
	public ImageDisplay( String name, double alpha, List< String > sources, String color, double[] contrastLimits, BlendingMode blendingMode )
	{
		super( name, alpha, sources );
		this.color = color;
		this.contrastLimits = contrastLimits;
		this.blendingMode = blendingMode;
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

	public BlendingMode getBlendingMode()
	{
		return blendingMode;
	}
}
