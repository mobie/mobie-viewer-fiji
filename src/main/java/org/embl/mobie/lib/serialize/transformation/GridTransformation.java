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
package org.embl.mobie.lib.serialize.transformation;

import net.imglib2.roi.RealMaskRealInterval;
import net.thisptr.jackson.jq.internal.misc.Strings;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.transform.ImageTransformer;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class GridTransformation extends AbstractGridTransformation
{
	// Serialization
	public List< List< String > > nestedSources;
	public List< List< String > > transformedNames;
	public boolean centerAtOrigin = true;

	public GridTransformation( List< List< String > > sources,
							   String nestedSources // to make this have a different erasure
	)
	{
		this.nestedSources = sources;
	}

	public GridTransformation( List< List< String > > sources,
							   List< int[] > positions,
							   String nestedSources// to make this have a different erasure
	)
	{
		this.nestedSources = sources;
		this.positions = positions;

		if ( positions.size() != sources.size() )
			throw new UnsupportedOperationException("GridTransformation Error: " +
					"The number of grid sources does not equal the number of grid positions.");
	}

	public GridTransformation( List< String > sources, List< int[] > positions )
	{
		this( sources );
		this.positions = positions;

		if ( positions.size() != sources.size() )
			throw new UnsupportedOperationException("GridTransformation Error: " +
					"The number of grid sources does not equal the number of grid positions.");
	}

	public GridTransformation( List< String > sources )
	{
		nestedSources = sources.stream()
				.map( source -> Collections.singletonList( source ) )
				.collect( Collectors.toList() );
	}

	public static List< ? extends Image< ? > > gridTransform(
			List< ? extends List< ? extends Image< ? > > >  nestedImages,
			@Nullable List< List< String > > nestedTransformedNames,
			List< int[] > positions,
			double[] tileRealDimensions,
			boolean centerAtOrigin,
			double[] withinTileOffset,
			final boolean transformInPlace )
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
					final double[] translation = new double[ 3 ];
					for ( int d = 0; d < 2; d++ )
						translation[ d ] = tileRealDimensions[ d ] * positions.get( finalGridIndex )[ d ] + withinTileOffset[ d ];

					List< String > transformedImageNames =
							nestedTransformedNames == null ?
									null : nestedTransformedNames.get( finalGridIndex );

					final List< ? extends Image< ? > > translatedImages =
							ImageTransformer.translate(
									images,
									transformedImageNames,
									centerAtOrigin,
									translation,
									transformInPlace );

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

    @NotNull
    public static List< ? extends Image< ? > > translateImages( GridTransformation gridTransformation )
    {
		List< ? extends List< ? extends Image< ? > > > nestedImages =
				gridTransformation.nestedSources.stream()
				.map( DataStore::getImageList )
				.collect( Collectors.toList() );

		// The size of a grid tile is the size of the
        // largest union mask of the images at the grid positions.
        double[] tileRealDimensions = new double[ 2 ];
        for ( List< ? extends Image< ? > > images : nestedImages )
        {
			List< String > imageNames = images.stream().map( Image::getName ).collect( Collectors.toList() );
			System.out.println("Computing grid tile size");
			System.out.println("  Images: " + Strings.join( ", ", imageNames ) );
			final RealMaskRealInterval unionMask = MoBIEHelper.union( images );
            final double[] realDimensions = MoBIEHelper.getRealDimensions( unionMask );
			System.out.println("  Union size: " +  Arrays.toString( realDimensions ) );

			// Update the overall grid tile size
			for ( int d = 0; d < 2; d++ )
                tileRealDimensions[ d ] = Math.max( realDimensions[ d ], tileRealDimensions[ d ] );
		}

        // Add a margin to the tiles
        for ( int d = 0; d < 2; d++ )
        {
            tileRealDimensions[ d ] = tileRealDimensions[ d ] * ( 1.0 + 2 * gridTransformation.margin );
        }

        // Compute the corresponding offset of where to place
        // the images within the tile
        final double[] offset = new double[ 2 ];
        for ( int d = 0; d < 2; d++ )
        {
            offset[ d ] = tileRealDimensions[ d ] * gridTransformation.margin;
        }

        final List< int[] > gridPositions = gridTransformation.positions == null ?
				createGridPositions( nestedImages.size() ) : gridTransformation.positions;

        final List< ? extends Image< ? > > transformedImages =
				gridTransform(
						nestedImages,
						gridTransformation.transformedNames,
						gridPositions,
						tileRealDimensions,
						gridTransformation.centerAtOrigin,
						offset,
						false );

        return transformedImages;
    }

    @Override
	public List< String > getSources()
	{
		final ArrayList< String > allSources = new ArrayList<>();
		for ( List< String > sourcesAtGridPosition : nestedSources )
			allSources.addAll( sourcesAtGridPosition );
		return allSources;
	}

	// Helper methods

	public static ArrayList< int[] > createGridPositions( int numPositions )
	{
		final int numX = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		ArrayList< int[] > positions = new ArrayList<>();
		int xPositionIndex = 0;
		int yPositionIndex = 0;
		for ( int gridIndex = 0; gridIndex < numPositions; gridIndex++ )
		{
			if ( xPositionIndex == numX )
			{
				xPositionIndex = 0;
				yPositionIndex++;
			}
			positions.add( new int[]{ xPositionIndex, yPositionIndex }  );
			xPositionIndex++;
		}
		return positions;
	}
}
