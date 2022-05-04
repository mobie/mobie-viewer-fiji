package org.embl.mobie.viewer.display;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.opacity.AdjustableOpacityColorConverter;
import net.imglib2.display.ColorConverter;
import org.embl.mobie.viewer.volume.ImageVolumeViewer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageSourceDisplay extends AbstractSourceDisplay
{
	// Serialization
	private List< String > sources;
	private String color;
	private double[] contrastLimits;
	private boolean showImagesIn3d;

	// Runtime
	public transient ImageSliceView imageSliceView;
	public transient ImageVolumeViewer imageVolumeViewer;

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

	public boolean showImagesIn3d()
	{
		return showImagesIn3d;
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
		this.sources.addAll( imageDisplay.sourceNameToSourceAndConverter.keySet() );

		final SourceAndConverter< ? > sourceAndConverter = imageDisplay.sourceNameToSourceAndConverter.values().iterator().next();

		setDisplaySettings( sourceAndConverter );

		// TODO - show images in 3d (currently not supported in viewer)
	}

	public ImageSourceDisplay( SourceAndConverter< ? > sourceAndConverter )
	{
		sources = Arrays.asList( sourceAndConverter.getSpimSource().getName() );
		setDisplaySettings( sourceAndConverter );
	}

	private void setDisplaySettings( SourceAndConverter< ? > sourceAndConverter )
	{
		final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );

		if( sourceAndConverter.getConverter() instanceof AdjustableOpacityColorConverter )
		{
			opacity = ( ( AdjustableOpacityColorConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		if ( sourceAndConverter.getConverter() instanceof ColorConverter)
		{
			// needs to be of form r=(\\d+),g=(\\d+),b=(\\d+),a=(\\d+)"
			color = ( ( ColorConverter ) sourceAndConverter.getConverter() ).getColor().toString();
			color = color.replaceAll("[()]", "");
		}

		contrastLimits = new double[2];
		contrastLimits[0] = converterSetup.getDisplayRangeMin();
		contrastLimits[1] = converterSetup.getDisplayRangeMax();

		blendingMode = (BlendingMode) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );
	}
}
