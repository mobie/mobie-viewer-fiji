package org.embl.mobie.viewer.view;

import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.MoBIEUtils;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.CropSourceTransformer;
import org.embl.mobie.viewer.transform.MaskedSource;
import org.embl.mobie.viewer.transform.MergedGridSource;
import org.embl.mobie.viewer.transform.SourceTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewFromSourceAndConverterCreator
{
	private final SourceAndConverter sourceAndConverter;

	public ViewFromSourceAndConverterCreator( SourceAndConverter sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}

	public View createView( String uiSelectionGroup )
	{
		final ArrayList< SourceDisplay > sourceDisplays = new ArrayList<>();
		final ArrayList< SourceTransformer > sourceTransformers = new ArrayList<>();
		final View view = new View( uiSelectionGroup, sourceDisplays, sourceTransformers, false );

		// recursively add all transformations
		// FIXME: in fact this will be the wrong order.
		addSourceTransformers( sourceAndConverter.getSpimSource(), sourceTransformers );

		Collections.reverse( sourceTransformers );

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

	private void addSourceTransformers( Source< ? > source, List< SourceTransformer > sourceTransformers )
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
				sourceTransformers.add( new AffineSourceTransformer( transformedSource ) );
			}
			addSourceTransformers( transformedSource.getWrappedSource(), sourceTransformers );
		}
		else if ( source instanceof MaskedSource )
		{
			final MaskedSource maskedSource = ( MaskedSource ) source;
			sourceTransformers.add( new CropSourceTransformer<>( maskedSource ) );
			addSourceTransformers( maskedSource.getWrappedSource(), sourceTransformers );
		}
		else if (  source instanceof LabelSource )
		{
			addSourceTransformers( (( LabelSource ) source).getWrappedSource(), sourceTransformers );
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
