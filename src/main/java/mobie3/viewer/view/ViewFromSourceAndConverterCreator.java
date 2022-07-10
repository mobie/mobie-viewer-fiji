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
package mobie3.viewer.view;

import bdv.SpimSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mobie3.viewer.bdv.render.BlendingMode;
import mobie3.viewer.color.LabelConverter;
import mobie3.viewer.color.OpacityAdjuster;
import mobie3.viewer.display.ImageDisplay;
import mobie3.viewer.display.SourceDisplay;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.MergedGridSource;
import mobie3.viewer.transform.AffineImageTransformer;
import mobie3.viewer.transform.ImageTransformer;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;

public class ViewFromSourceAndConverterCreator
{
	private final SourceAndConverter sourceAndConverter;

	// display settings
	private double opacity;
	private BlendingMode blendingMode;
	private double[] contrastLimits;
	private String color;

	// TODO: not used, what was this intended for?
	public ViewFromSourceAndConverterCreator( SourceAndConverter sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}

	public View getView()
	{
		final ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
		final ArrayList< ImageTransformer > imageTransformers = new ArrayList<>();
		final View view = new View( "uiSelectionGroup", sourceDisplays, imageTransformers, false );

		// recursively add all transformations
		// FIXME: in fact this will be the wrong order.
		addSourceTransformers( sourceAndConverter.getSpimSource(), imageTransformers );

		if ( sourceAndConverter.getConverter() instanceof LabelConverter )
		{
			throwError( sourceAndConverter.getConverter() );
		}
		else
		{
			sourceDisplays.add( new ImageDisplay( sourceAndConverter ) );
		}

		return view;
	}

	private void createSourceDisplay( SourceAndConverter< ? > sourceAndConverter  )
	{
		final Converter< ?, ARGBType > converter = sourceAndConverter.getConverter();
		final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );

		if( converter instanceof OpacityAdjuster )
			opacity = ( ( OpacityAdjuster ) converter ).getOpacity();

		if ( converter instanceof ColorConverter )
		{
			// needs to be of form r=(\\d+),g=(\\d+),b=(\\d+),a=(\\d+)"
			color = ( ( ColorConverter ) converter ).getColor().toString();
			color = color.replaceAll("[()]", "");
		}

		contrastLimits = new double[2];
		contrastLimits[0] = converterSetup.getDisplayRangeMin();
		contrastLimits[1] = converterSetup.getDisplayRangeMax();

		blendingMode = ( BlendingMode ) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );
	}

	private void addSourceTransformers( Source< ? > source, List< ImageTransformer > imageTransformers )
	{
		if ( source instanceof SpimSource )
		{
			return;
		}
		else if ( source instanceof TransformedSource )
		{
			final TransformedSource transformedSource = ( TransformedSource ) source;

			AffineTransform3D fixedTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( fixedTransform );
			if ( ! fixedTransform.isIdentity() )
			{
				imageTransformers.add( new AffineImageTransformer( transformedSource ) );
			}

			final Source< ? > wrappedSource = transformedSource.getWrappedSource();

			addSourceTransformers( wrappedSource, imageTransformers );
		}
		else if (  source instanceof BoundarySource )
		{
			final Source< ? > wrappedSource = (( BoundarySource ) source).getWrappedSource();

			addSourceTransformers( wrappedSource, imageTransformers );
		}
		else if (  source instanceof MergedGridSource )
		{
			throwError( source );
		}
		else if (  source instanceof ResampledSource )
		{
			throwError( source );
		}
		else
		{
			throwError( source );
		}
	}

	private void throwError( Object object )
	{
		throw new UnsupportedOperationException( "Cannot yet create a view from a " + object.getClass().getName() );
	}

}
