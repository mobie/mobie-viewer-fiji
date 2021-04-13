package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;

import java.util.ArrayList;
import java.util.List;

public class ImageDisplay extends SourceDisplay
{
	// Serialization
	private String color;
	private double[] contrastLimits;
	private BlendingMode blendingMode;
	private boolean showImagesIn3d;

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
