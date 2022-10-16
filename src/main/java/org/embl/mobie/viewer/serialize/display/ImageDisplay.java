/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer.serialize.display;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.color.opacity.AdjustableOpacityColorConverter;
import org.embl.mobie.viewer.volume.ImageVolumeViewer;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ImageDisplay< T extends NumericType< T > > extends AbstractDisplay< T >
{
	// Serialization
	private List< String > sources;
	private String color;
	private double[] contrastLimits;
	private boolean showImagesIn3d;
	private Double[] resolution3dView;

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

	public Double[] getResolution3dView() { return resolution3dView; }

	public boolean showImagesIn3d()
	{
		return showImagesIn3d;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	public ImageDisplay() {}

	// Constructor for serialization
	public ImageDisplay( String name, double opacity, List< String > sources, String color, double[] contrastLimits, BlendingMode blendingMode, boolean showImagesIn3d ) {
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
	public ImageDisplay( ImageDisplay< ? > imageDisplay )
	{
		this.name = imageDisplay.name;
		this.sources = new ArrayList<>();
		this.sources.addAll( imageDisplay.getSourceAndConverters().stream().map( sac -> sac.getSpimSource().getName() ).collect( Collectors.toList() ) );
		setDisplaySettings( imageDisplay.getSourceAndConverters().get( 0 ) );

		if ( imageDisplay.imageVolumeViewer != null )
		{
			this.showImagesIn3d = imageDisplay.imageVolumeViewer.getShowImages();

			double[] voxelSpacing = imageDisplay.imageVolumeViewer.getVoxelSpacing();
			if ( voxelSpacing != null ) {
				resolution3dView = new Double[voxelSpacing.length];
				for (int i = 0; i < voxelSpacing.length; i++) {
					resolution3dView[i] = voxelSpacing[i];
				}
			}
		}

		if ( imageDisplay.imageSliceView != null ) {
			visible = imageDisplay.imageSliceView.isVisible();
		}
	}

	public ImageDisplay( SourceAndConverter< ? > sourceAndConverter )
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

		blendingMode = ( BlendingMode ) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.class.getName() );
	}
}
