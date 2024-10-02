/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.serialize.display;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.bdv.view.ImageSliceView;
import org.embl.mobie.lib.color.OpacityHelper;
import org.embl.mobie.lib.color.opacity.MoBIEColorConverter;
import org.embl.mobie.lib.volume.ImageVolumeViewer;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ImageDisplay< T extends NumericType< T > > extends AbstractDisplay< T >
{
	// Serialization
	protected List< String > sources;
	protected String color;
	protected double[] contrastLimits;
	protected boolean showImagesIn3d;
	protected Double[] resolution3dView;
	protected boolean invert; // TODO add to spec

	// Runtime
	public transient ImageSliceView< ?> imageSliceView;
	public transient ImageVolumeViewer imageVolumeViewer;

	public ImageDisplay(
			String name,
			List< String > sources,
			String color,
			double[] contrastLimits )
	{
		this( name, sources, color, contrastLimits, false, null );
	}

	public ImageDisplay(
			String name,
			List< String > sources,
			String color,
			double[] contrastLimits,
			boolean showImagesIn3d,
			Double[] resolution3dView )
	{
		this.name = name;
		this.sources = sources;
		this.color = color;
		this.contrastLimits = contrastLimits;
		this.showImagesIn3d = showImagesIn3d;
		this.resolution3dView = resolution3dView;
	}

	// Gson deserialization
	public ImageDisplay() {}

	public ImageDisplay( String name, String sourceName )
	{
		this.name = name;
		this.sources = Collections.singletonList( sourceName );
	}

	// Project creator serialization
	public ImageDisplay(
			String name,
			double opacity,
			List< String > sources,
			String color,
			double[] contrastLimits,
			BlendingMode blendingMode,
			boolean showImagesIn3d )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.color = color;
		this.contrastLimits = contrastLimits;
		this.blendingMode = blendingMode;
		this.showImagesIn3d = showImagesIn3d;
	}

	/*
	 * Create a serializable copy
	 */
	public ImageDisplay( ImageDisplay< ? > imageDisplay )
	{
		this.name = imageDisplay.name;
		this.sources = new ArrayList<>();
		this.sources.addAll( imageDisplay.sourceAndConverters().stream().map( sac -> sac.getSpimSource().getName() ).collect( Collectors.toList() ) );

		setDisplaySettings( imageDisplay.sourceAndConverters().get( 0 ) );

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


	public String getColor()
	{
		return color;
	}

	public double[] getContrastLimits()
	{
		return contrastLimits;
	}

	public boolean invert()
	{
		return invert;
	}

	@Override
	public BlendingMode getBlendingMode()
	{
		return blendingMode != null ? blendingMode : BlendingMode.Sum;
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


	public void setDisplaySettings( SourceAndConverter< ? > sourceAndConverter )
	{
		final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );

		Converter< ?, ARGBType > converter = sourceAndConverter.getConverter();

		opacity = OpacityHelper.getOpacity( converter );

		if ( converter instanceof ColorConverter)
		{
			// needs to be of form r=(\\d+),g=(\\d+),b=(\\d+),a=(\\d+)"
			color = ( ( ColorConverter ) converter ).getColor().toString();
			color = color.replaceAll("[()]", "");
		}

		if ( converter instanceof MoBIEColorConverter )
		{
			invert = ( ( MoBIEColorConverter ) converter ).invert();
		}

		contrastLimits = new double[2];
		contrastLimits[0] = converterSetup.getDisplayRangeMin();
		contrastLimits[1] = converterSetup.getDisplayRangeMax();

		blendingMode = ( BlendingMode ) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.class.getName() );
	}
}
