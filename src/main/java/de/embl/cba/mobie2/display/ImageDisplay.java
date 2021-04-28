package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.bdv.ImageSliceView;
import de.embl.cba.mobie2.color.opacity.AdjustableOpacityColorConverter;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;

import java.util.ArrayList;

public class ImageDisplay extends Display
{
	// Serialization
	private String color;
	private double[] contrastLimits;
	private BlendingMode blendingMode;
	private boolean showImagesIn3d;

	// Runtime
	public transient ImageSliceView imageSliceView;

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

	/**
	 * Create a serializable copy
	 *
	 * @param imageDisplay
	 */
	public ImageDisplay( ImageDisplay imageDisplay )
	{
		this.sources = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			sources.add( sourceAndConverter.getSpimSource().getName() );
		}

		final SourceAndConverter< ? > sourceAndConverter = imageDisplay.sourceAndConverters.get( 0 );
		if( sourceAndConverter.getConverter() instanceof AdjustableOpacityColorConverter )
		{
			this.opacity = ( ( AdjustableOpacityColorConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		// TODO
		//   - fetch the rest and construct an ImageDisplay
	}
}
