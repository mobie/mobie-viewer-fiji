/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.transform;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.InterpolatedAffineTransformation;
import org.embl.mobie.lib.serialize.transformation.TimepointsTransformation;
import org.embl.mobie.lib.table.AnnData;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class ImageTransformer
{
	public static Image< ? > affineTransform( Image< ? > image, AffineTransformation affineTransformation )
	{
		return affineTransform( image,
				affineTransformation.getAffineTransform3D(),
				affineTransformation.getTransformedImageName( image.getName() ) );
	}

	public static Image< ? > affineTransform( Image< ? > image, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		if( transformedImageName == null || image.getName().equals( transformedImageName ) )
		{
			// in place transformation
			image.transform( affineTransform3D  );
			return image;
		}

		if ( image instanceof AnnotationLabelImage )
		{
			return createTransformedAnnotatedLabelImage( ( AnnotationLabelImage ) image, affineTransform3D, transformedImageName );
		}
		else if ( image instanceof AnnotationImage )
		{
			throw new UnsupportedOperationException( "Creating a transformed duplicate of an " + image.getClass() + " is currently not supported." );
		}
		else
		{
			return new AffineTransformedImage<>( image, transformedImageName, affineTransform3D );
		}
	}

	public static Image< ? > timeTransform( Image< ? > image, TimepointsTransformation transformation )
	{
		String transformedImageName = transformation.getTransformedImageName( image.getName() );

		return new TimepointsTransformedImage<>(
				image,
				transformedImageName == null ? image.getName() : transformedImageName,
				transformation.getTimepointsMapping(),
				transformation.isKeep() );
	}

	public static Image< ? > interpolatedAffineTransform( Image< ? > image, InterpolatedAffineTransformation transformation )
	{
		String transformedImageName = transformation.getTransformedImageName( image.getName() );

		AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( image.getSourcePair().getSource(), 0, 0 );
		Interpolated3DAffineRealTransform interpolatedTransform = new Interpolated3DAffineRealTransform( sourceTransform.inverse() );
		interpolatedTransform.addTransforms( transformation.getTransforms() );

		return new RealTransformedImage<>(
				image,
				transformedImageName == null ? image.getName() : transformedImageName,
				interpolatedTransform );
	}

	private static < A extends Annotation, TA extends A > DefaultAnnotationLabelImage< TA > createTransformedAnnotatedLabelImage( AnnotationLabelImage< A > annotatedLabelImage, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		final Image< ? extends IntegerType< ? > > labelImage = annotatedLabelImage.getLabelImage();

		final AnnData< A > annData = annotatedLabelImage.getAnnData();

		final AnnotationAffineTransformer< A, TA > affineTransformer = new AnnotationAffineTransformer<>( affineTransform3D );

		TransformedAnnData< A, TA > transformedAnnData = new TransformedAnnData<>( annData, affineTransformer );

		final DefaultAnnotationAdapter< TA > annotationAdapter = new DefaultAnnotationAdapter<>( transformedAnnData );

		final Image< ? extends IntegerType< ? > > transformedLabelImage = ( Image< ? extends IntegerType< ? > > ) affineTransform( labelImage, affineTransform3D, transformedImageName );

		final DefaultAnnotationLabelImage< TA > transformedAnnotatedImage = new DefaultAnnotationLabelImage< TA >( transformedLabelImage, transformedAnnData, annotationAdapter );

		return transformedAnnotatedImage;
	}

	public static List< ? extends Image< ? > > gridTransform( List< List< ? extends Image< ? > > > nestedImages, @Nullable List< List< String > > nestedTransformedNames, List< int[] > positions, double[] tileRealDimensions, boolean centerAtOrigin, double[] withinTileOffset )
	{
		final CopyOnWriteArrayList< ? extends Image< ? > > transformedImages = new CopyOnWriteArrayList<>();

		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		final int numGridPositions = nestedImages.size();
		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			futures.add( ThreadHelper.executorService.submit( () -> {
				try
				{
					final List< ? extends Image< ? > > images = nestedImages.get( finalGridIndex );
					final double[] translation = new double[ 2 ];
					for ( int d = 0; d < 2; d++ )
						translation[ d ] = tileRealDimensions[ d ] * positions.get( finalGridIndex )[ d ] + withinTileOffset[ d ];

					List< String > transformedImageNames = nestedTransformedNames == null ? null : nestedTransformedNames.get( finalGridIndex );

					final List< ? extends Image< ? > > translatedImages = translate( images, transformedImageNames, centerAtOrigin, translation[ 0 ], translation[ 1 ] );

					transformedImages.addAll( ( List ) translatedImages );
				}
				catch ( Exception e )
				{
					throw ( e );
				}
			} ) );
		}
		ThreadHelper.waitUntilFinished( futures );

		return transformedImages;
	}

	public static ArrayList< Image< ? > > translate( List< ? extends Image< ? > > images, @Nullable List< String > transformedNames, boolean centerAtOrigin, double translationX, double translationY )
	{
		final ArrayList< Image< ? > > translatedImages = new ArrayList<>();

		for ( Image< ? > image : images )
		{
			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform( translationX, translationY, image, centerAtOrigin );

			if ( transformedNames == null )
			{
				// in place transformation
				image.transform( translationTransform );
				translatedImages.add( image );
			}
			else
			{
				// create a new transformed image
				final Image< ? > transformedImage = affineTransform( image, translationTransform, transformedNames.get( images.indexOf( image ) ) );
				translatedImages.add( transformedImage );
			}
		}

		return translatedImages;
	}
}
