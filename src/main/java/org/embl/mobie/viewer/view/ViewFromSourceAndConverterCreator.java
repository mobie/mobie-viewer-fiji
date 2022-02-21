package org.embl.mobie.viewer.view;

import bdv.SpimSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.MergedGridSource;
import org.embl.mobie.viewer.transform.SourceTransformer;
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

	public ViewFromSourceAndConverterCreator( SourceAndConverter sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}

	public View getView()
	{
		final ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
		final ArrayList< SourceTransformer > sourceTransformers = new ArrayList<>();
		final View view = new View( "uiSelectionGroup", sourceDisplays, sourceTransformers, false );

		// recursively add all transformations
		// FIXME: in fact this will be the wrong order.
		addSourceTransformers( sourceAndConverter.getSpimSource(), sourceTransformers );

		if ( sourceAndConverter.getConverter() instanceof LabelConverter )
		{
			throwError( sourceAndConverter.getConverter() );
		}
		else
		{
			sourceDisplays.add( new ImageSourceDisplay( sourceAndConverter ) );
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

		blendingMode = (BlendingMode) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE );



	}

	private void addSourceTransformers( Source< ? > source, List< SourceTransformer > sourceTransformers )
	{
		if ( source instanceof SpimSource )
		{
			return;
		}
		else if ( source instanceof TransformedSource )
		{
			final AffineSourceTransformer sourceTransformer = ViewHelpers.createAffineSourceTransformer( ( TransformedSource< ? > ) source );
			if ( sourceTransformer != null )
				sourceTransformers.add( sourceTransformer );

			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			addSourceTransformers( wrappedSource, sourceTransformers );
		}
		else if (  source instanceof LabelSource )
		{
			final Source< ? > wrappedSource = (( LabelSource ) source).getWrappedSource();

			addSourceTransformers( wrappedSource, sourceTransformers );
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
