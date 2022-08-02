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

import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.StitchedImage;
import org.embl.mobie.viewer.transform.AbstractGridTransformation;
import org.embl.mobie.viewer.transform.TransformedGridTransformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MergedGridTransformation< T extends NumericType< T > > extends AbstractGridTransformation< T >
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected boolean centerAtOrigin = false; // TODO: should actually be true, but: https://github.com/mobie/mobie-viewer-fiji/issues/685#issuecomment-1108179599
	protected boolean encodeSource = false; // true for the first time label images are encoded // TODO: (we can remove this now).

	// Runtime
	private transient Image< T > stitchedImage;

	public Image< T > apply( List< Image< T > > images )
	{
		if ( stitchedImage == null )
		{
			if ( positions == null )
				autoSetPositions();

			stitchedImage = new StitchedImage<>( images, positions, mergedGridSourceName, TransformedGridTransformation.RELATIVE_CELL_MARGIN );

			// transform the individual stitched images as well
			transform( images );
		}

		return stitchedImage;
	}

	private void transform( List< Image< T > > images )
	{
		final TransformedGridTransformation< T > gridTransformation = new TransformedGridTransformation<>();
		gridTransformation.positions = positions;
		final ArrayList< List< Image< T > > > nestedImages = new ArrayList<>();
		for ( Image< T > image : images )
		{
			final ArrayList< Image< T > > imagesAtGridPosition = new ArrayList<>();
			// TODO: create placeholder image
			//   or image with a metadata image
			imagesAtGridPosition.add( image );
			nestedImages.add( imagesAtGridPosition );
		}

		// in case of stitched images, also transform the
		// images contained within the stitched images,
		// such that all images have consistent positions.
		for ( List< Image< T > > imagesAtGridPosition : nestedImages )
		{
			for ( Image< T > image : imagesAtGridPosition )
			{
				if ( image instanceof StitchedImage )
				{
					final List< String > stitchedImageNames = ( ( StitchedImage< ?, ? > ) image ).getStitchedImages().stream().map( i -> i.getName() ).collect( Collectors.toList() );
					// TODO: possibly fetch them from a placeholder store
					imagesAtGridPosition.add( ( Image< T > ) ImageStore.getImages( stitchedImageNames ) );
				}
			}
		}

		final List< Image< T > > transformed = gridTransformation.apply( nestedImages );
		// TODO, possibly put them into a placeholder store.
		ImageStore.putImages( transformed );
	}

	@Override
	public List< String > getTargetImageNames()
	{
		return sources;
	}
}
