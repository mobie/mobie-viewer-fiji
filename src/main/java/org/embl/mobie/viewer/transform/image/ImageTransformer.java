package org.embl.mobie.viewer.transform.image;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.ThreadHelper;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.transform.AnnotationAffineTransformer;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.embl.mobie.viewer.transform.TransformedAnnData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class ImageTransformer
{
	// Note that the Image Type may change, e.g. if this is an annotated label image
	// TODO
	//  Is there a cleaner solution? For normal images the type would not change.
	//  also for annotated images the type would only change
	//  if transformedImage != image.getName(), because otherwise we would transform in place
	//  Maybe two methods, one for really creating a new image and one for in place transformation?
	public static Image< ? > transform( Image< ? > image, AffineTransform3D affineTransform3D, @Nullable String transformedImageName )
	{
		if ( transformedImageName == null )
		{
			// in place transformation
			//System.out.println("Transform " + image.getName() + ": " + affineTransform3D.toString() );
			image.transform( affineTransform3D );
			return image;
		}

		// create new transformed image

		if ( image instanceof AnnotatedLabelImage )
		{
			return createTransformedAnnotatedLabelImage( ( AnnotatedLabelImage ) image, affineTransform3D,  transformedImageName );
		}

		return new AffineTransformedImage<>( image, transformedImageName, affineTransform3D );
	}

	private static < A extends Annotation, TA extends A > DefaultAnnotatedLabelImage< TA > createTransformedAnnotatedLabelImage( AnnotatedLabelImage< A > annotatedLabelImage, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		final Image< ? extends IntegerType< ? > > labelImage = annotatedLabelImage.getLabelImage();

		final AnnData< A > annData = annotatedLabelImage.getAnnData();

		final AnnotationAffineTransformer< A, TA > affineTransformer = new AnnotationAffineTransformer<>( affineTransform3D );

		TransformedAnnData< A, TA > transformedAnnData = new TransformedAnnData<>( annData, affineTransformer );

		final Image< ? extends IntegerType< ? > > transformedLabelImage = ( Image< ? extends IntegerType< ? > > ) transform( labelImage, affineTransform3D, transformedImageName );

		// FIXME I guess I have to get the AnnotationAdapter
		//   from the AnnotatedLabelImage?!
		final DefaultAnnotatedLabelImage< TA > transformedAnnotatedImage = new DefaultAnnotatedLabelImage< TA >( transformedLabelImage, transformedAnnData, new DefaultAnnotationAdapter( annData ) );

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

			String transformedName = transformedNames == null ? null : transformedNames.get( images.indexOf( image ) );

			final Image< ? > transformedImage = transform( image, translationTransform, transformedName );

			translatedImages.add( transformedImage );
		}

		return translatedImages;
	}
}
