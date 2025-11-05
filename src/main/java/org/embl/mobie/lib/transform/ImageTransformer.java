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
package org.embl.mobie.lib.transform;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.serialize.transformation.*;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.util.MoBIEHelper;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageTransformer
{
	public static Image< ? > affineTransform( Image< ? > image, AffineTransformation affineTransformation )
	{
		String transformedImageName = getTransformedImageName( affineTransformation.getTransformedImageName( image.getName() ), image.getName() );

		// FIXME The below will destroy the Transformation History of this image
		//       Not sure why we had this? For performance??
//		if( transformedImageName == null || image.getName().equals( transformedImageName ) )
//		{
//			// in place transformation
//			image.transform( affineTransformation.getAffineTransform3D()  );
//			return image;
//		}

		if ( image instanceof AnnotatedLabelImage )
		{
			return createTransformedAnnotatedLabelImage(
					( AnnotatedLabelImage ) image,
					affineTransformation );
		}
		else if ( image instanceof AnnotationImage )
		{
			throw new UnsupportedOperationException( "Creating a transformed duplicate of an " + image.getClass() + " is currently not supported." );
		}
		else
		{
			AffineTransformedImage< ? > affineTransformedImage =
					new AffineTransformedImage<>(
							image,
							transformedImageName,
							affineTransformation );

			return affineTransformedImage;
		}
	}

	private static String getTransformedImageName( String transformedImageName, String imageName )
	{
		return transformedImageName == null ? imageName : transformedImageName;
	}

	public static Image< ? > tpsTransform( Image< ? > image, TpsTransformation transformation )
	{
		String transformedImageName = getTransformedImageName( transformation.getTransformedImageName( image.getName() ), image.getName() );

		if ( image instanceof AnnotatedLabelImage )
		{
			throw new RuntimeException( "Thin plate spline transformations of label images are not yet implemented...");
//			return createTransformedAnnotatedLabelImage(
//					( AnnotatedLabelImage ) image,
//					affineTransformation );
		}
		else if ( image instanceof AnnotationImage )
		{
			throw new UnsupportedOperationException( "Creating a transformed duplicate of an " + image.getClass() + " is currently not supported." );
		}
		else
		{
			TpsTransformedImage< ? > tpsTransformedImage =
					new TpsTransformedImage<>(
							image,
							transformedImageName,
							transformation );

			return tpsTransformedImage;
		}
	}

	public static Image< ? > timeTransform( Image< ? > image, TimepointsTransformation transformation )
	{
		String transformedImageName = transformation.getTransformedImageName( image.getName() );

		TimepointsTransformedImage< ? > transformedImage = new TimepointsTransformedImage<>(
				image,
				transformedImageName == null ? image.getName() : transformedImageName,
				transformation.getTimepointsMapping(),
				transformation.isKeep() );

		// FIXME: This should happen in the constructor
		transformedImage.setTransformation( transformation );

		return transformedImage;

	}

	public static Image< ? > interpolatedAffineTransform( Image< ? > image, InterpolatedAffineTransformation transformation )
	{
		// FIXME: Move all this code into the constructor of RealTransformedImage
		String transformedImageName = transformation.getTransformedImageName( image.getName() );

		AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( image.getSourcePair().getSource(), 0, 0 );
		InterpolatedAffineRealTransform interpolatedTransform = new InterpolatedAffineRealTransform( transformation.getName(), sourceTransform.inverse() );
		interpolatedTransform.addTransforms( transformation.getTransforms() );

		RealTransformedImage< ? > realTransformedImage =
				new RealTransformedImage<>(
					image,
					transformedImageName == null ? image.getName() : transformedImageName,
					interpolatedTransform );

		// FIXME: This should be done in the constructor of RealTransformedImage !
		realTransformedImage.setTransformation( transformation );

		return realTransformedImage;
	}

	private static < A extends Annotation, TA extends A > DefaultAnnotatedLabelImage< ? > createTransformedAnnotatedLabelImage(
			AnnotatedLabelImage< A > annotatedLabelImage,
			AffineTransformation affineTransformation )
	{
		final Image< ? extends IntegerType< ? > > labelImage = annotatedLabelImage.getLabelImage();
		final Image< ? extends IntegerType< ? > > transformedLabelImage =
				( Image< ? extends IntegerType< ? > > ) affineTransform( labelImage, affineTransformation );
		final AnnData< A > annData = annotatedLabelImage.getAnnData();

		AnnotationAdapter< A > annotationAdapter = annotatedLabelImage.getAnnotationAdapter();

		if ( annotationAdapter instanceof LazyAnnotatedSegmentAdapter )
		{
			// There are no annotations with coordinates,
			// thus we do not need to transform them.
			return new DefaultAnnotatedLabelImage< A >( transformedLabelImage, annData, annotationAdapter );
		}
		else
		{
			final AnnotationAffineTransformer< A, TA > affineTransformer =
					new AnnotationAffineTransformer<>( affineTransformation.getAffineTransform3D() );

			TransformedAnnData< A, TA > transformedAnnData = new TransformedAnnData<>( annData, affineTransformer );

			AnnotationAdapter< TA > newAnnotationAdapter =
					new DefaultAnnotationAdapter<>(
							transformedAnnData,
							annotatedLabelImage.getName() );

			return new DefaultAnnotatedLabelImage< TA >( transformedLabelImage, transformedAnnData, newAnnotationAdapter );
		}
	}

	public static ArrayList< Image< ? > > translate(
			List< ? extends Image< ? > > images,
			@Nullable List< String > transformedNames,
			boolean centerAtOrigin,
			final double[] translation,
			boolean transformInPlace )
	{
		final ArrayList< Image< ? > > translatedImages = new ArrayList<>();

		// Use the first image for determining the centre position
		AffineTransform3D translationTransform =
				MoBIEHelper.createTranslationTransform(
						images.get( 0 ),
						centerAtOrigin,
						translation );

		for ( Image< ? > image : images )
		{

			if ( transformInPlace )
			{
				// in place transformation (only used for Stitched Images)
				// TODO: Check whether this is needed or could be get rid off
				image.transform( translationTransform );
				translatedImages.add( image );
			}
			else
			{
				// create a new transformed image
				String transformedImageName = transformedNames == null ?
						image.getName() :
						transformedNames.get( images.indexOf( image ) );

				AffineTransformation affineTransformation = new AffineTransformation(
							"Translation",
							translationTransform.getRowPackedCopy(),
							Collections.singletonList( image.getName() ),
							Collections.singletonList( transformedImageName )
							);

				final Image< ? > transformedImage = affineTransform( image, affineTransformation );
				translatedImages.add( transformedImage );
			}
		}

		return translatedImages;
	}

	public static void transformImages( Transformation transformation, List< ? extends Image< ? > > images )
	{
		if ( transformation instanceof AffineTransformation )
		{
			final AffineTransformation affineTransformation = ( AffineTransformation ) transformation;

			for ( Image< ? > image : images )
			{
				Image< ? > transformedImage = affineTransform( image, affineTransformation );
				DataStore.addImage( transformedImage );
			}
		}
		else if ( transformation instanceof TpsTransformation )
		{
			TpsTransformation tpsTransformation = ( TpsTransformation ) transformation;

			for ( Image< ? > image : images )
			{
				Image< ? > transformedImage = tpsTransform( image, tpsTransformation );
				DataStore.addImage( transformedImage );
			}

		}
		else if ( transformation instanceof CropTransformation )
		{
			final CropTransformation cropTransformation = ( CropTransformation ) transformation;

			for ( Image< ? > image : images )
			{
				// TODO: move this into the ImageTransformer class
				final CroppedImage< ? > croppedImage = new CroppedImage<>(
						image,
						cropTransformation.getTransformedImageName( image.getName() ),
						cropTransformation.min,
						cropTransformation.max,
						cropTransformation.centerAtOrigin );
				DataStore.addImage( croppedImage );
			}
		}
		else if ( transformation instanceof TimepointsTransformation )
		{
			final TimepointsTransformation timepointsTransformation = ( TimepointsTransformation ) transformation;

			for ( Image< ? > image : images )
			{
				DataStore.addImage( timeTransform( image, timepointsTransformation ) );
			}
		}
		else if ( transformation instanceof InterpolatedAffineTransformation )
		{
			InterpolatedAffineTransformation interpolatedAffineTransformation = ( InterpolatedAffineTransformation ) transformation;

			for ( Image< ? > image : images )
			{
				DataStore.addImage( interpolatedAffineTransform( image, interpolatedAffineTransformation ) );
			}
		}
		else
		{
			throw new UnsupportedOperationException( "Transformations of type " + transformation.getClass().getName() + " are not yet implemented.");
		}
	}
}
