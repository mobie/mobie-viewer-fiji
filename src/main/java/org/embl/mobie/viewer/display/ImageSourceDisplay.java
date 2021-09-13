package org.embl.mobie.viewer.display;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.opacity.AdjustableOpacityColorConverter;
import net.imglib2.display.ColorConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;

public class ImageSourceDisplay extends AbstractSourceDisplay
{
	// Serialization
	private List< String > sources;
	private String color;
	private double[] contrastLimits;
	private BlendingMode blendingMode;
	private boolean showImagesIn3d;

	// Runtime
	public transient ImageSliceView imageSliceView;

	// Getters for serialised fields
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

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	public ImageSourceDisplay() {}

	// Constructor for serialization
	public ImageSourceDisplay( String name, double opacity, List< String > sources, String color, double[] contrastLimits, BlendingMode blendingMode, boolean showImagesIn3d ) {
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.color = color;
		this.contrastLimits = contrastLimits;
		this.blendingMode = blendingMode;
		this.showImagesIn3d = showImagesIn3d;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param imageDisplay
	 */
	public ImageSourceDisplay( ImageSourceDisplay imageDisplay )
	{
		this.name = imageDisplay.name;
		this.sources = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			sources.add( sourceAndConverter.getSpimSource().getName() );
		}

		final SourceAndConverter< ? > sourceAndConverter = imageDisplay.sourceAndConverters.get( 0 );
		final ConverterSetup converterSetup = SourceAndConverterServices.getBdvDisplayService().getConverterSetup( sourceAndConverter );

		if( sourceAndConverter.getConverter() instanceof AdjustableOpacityColorConverter )
		{
			this.opacity = ( ( AdjustableOpacityColorConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		if ( sourceAndConverter.getConverter() instanceof ColorConverter)
		{
			// needs to be of form r=(\\d+),g=(\\d+),b=(\\d+),a=(\\d+)"
			String colorString = ( ( ColorConverter ) sourceAndConverter.getConverter() ).getColor().toString();
			colorString = colorString.replaceAll("[()]", "");
			this.color = colorString;
		}

		double[] contrastLimits = new double[2];
		contrastLimits[0] = converterSetup.getDisplayRangeMin();
		contrastLimits[1] = converterSetup.getDisplayRangeMax();
		this.contrastLimits = contrastLimits;

		this.blendingMode = (BlendingMode) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );

		// TODO - show images in 3d (currently not supported in viewer)
	}
}
