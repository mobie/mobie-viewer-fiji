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
package org.embl.mobie.viewer.transform.image;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.ThreadHelper;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.transform.ImageTransformer;
import org.embl.mobie.viewer.transform.TransformHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class GridTransformer
{
	public GridTransformer()
	{
	}

	public List< ? extends Image< ? > > getTransformedImages( List< List< ? extends Image< ? > > > nestedImages, @Nullable List< List< String > > nestedTransformedNames, List< int[] > positions, double[] tileRealDimensions, boolean centerAtOrigin, double[] withinTileOffset )
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

					List< String > transformedImageNames = nestedTransformedNames == null ? images.stream().map( image -> image.getName() ).collect( Collectors.toList() ) : nestedTransformedNames.get( finalGridIndex );
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

	private ArrayList< Image< ? > > translate( List< ? extends Image< ? > > images, List< String > transformedNames, boolean centerAtOrigin, double translationX, double translationY )
	{
		final ArrayList< Image< ? > > translatedImages = new ArrayList<>();

		for ( Image< ? > image : images )
		{
			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform( translationX, translationY, image, centerAtOrigin );

			final Image< ? > transformedImage =
					new ImageTransformer( image ).getTransformedImage(
							translationTransform,
							transformedNames.get( images.indexOf( image ) ) );

			translatedImages.add( transformedImage );
		}

		return translatedImages;
	}
}
