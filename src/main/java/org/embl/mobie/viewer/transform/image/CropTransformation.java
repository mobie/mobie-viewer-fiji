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

import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import org.embl.mobie.viewer.source.DefaultImage;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.transform.AbstractImageTransformation;
import org.embl.mobie.viewer.transform.TransformHelper;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.List;
import java.util.Map;

public class CropTransformation< T > extends AbstractImageTransformation< T, T >
{
	protected double[] min;
	protected double[] max;
	protected boolean centerAtOrigin = false;

	@Override
	public Image< T > apply( Image< T > image )
	{
		final Source< T > source = image.getSourcePair().getSource();
		final Source< ? extends Volatile< T > > volatileSource = image.getSourcePair().getVolatileSource();

		// determine number of voxels for resampling
		// the current method may over-sample quite a bit
		final double smallestVoxelSize = getSmallestVoxelSize( source );
		// slightly enlarge the crop
		// important to deal with quasi 2D images or crops
		final double[] minMinusVoxelSize = new double[ 3 ];
		final double[] maxPlusVoxelSize = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			minMinusVoxelSize[ d ] = min[ d ] - smallestVoxelSize;
			maxPlusVoxelSize[ d ] = max[ d ] + smallestVoxelSize;
		}
		final FinalVoxelDimensions croppedSourceVoxelDimensions = new FinalVoxelDimensions( source.getVoxelDimensions().unit(), smallestVoxelSize, smallestVoxelSize, smallestVoxelSize );
		int[] numVoxels = getNumVoxels( smallestVoxelSize, maxPlusVoxelSize, minMinusVoxelSize );
		SourceAndConverter< ? > cropModel = new EmptySourceAndConverterCreator("Model", new FinalRealInterval( minMinusVoxelSize, maxPlusVoxelSize ), numVoxels[ 0 ], numVoxels[ 1 ], numVoxels[ 2 ], croppedSourceVoxelDimensions ).get();

		// Resample
		//
		String transformedName = getTransformedName( image );
		Source resampledSource =
				new ResampledSource(
						source,
						cropModel.getSpimSource(),
						transformedName,
						false,
						false,
						false,
						0);


		Source volatileResampledSource =
				new ResampledSource(
						volatileSource,
						cropModel.getSpimSource(),
						transformedName,
						false,
						false,
						false,
						0);

		if ( centerAtOrigin )
		{
			// TODO
			throw new UnsupportedOperationException("Cannot yet apply centerAtOrigin");
			//croppedSourceAndConverter = TransformHelper.centerAtOrigin( croppedSourceAndConverter );
		}

		return new DefaultImage<>( resampledSource, volatileResampledSource, transformedName );
	}

	@Override
	public List< String > getTargetImageNames()
	{
		return sources;
	}

	private int[] getNumVoxels( double smallestVoxelSize, double[] max, double[] min )
	{
		int[] numVoxels = new int[ 3 ];
		for ( int d = 0; d < 3; d++ )
			numVoxels[ d ] = Math.max ( (int) Math.ceil( ( max[ d ] - min[ d ] ) / smallestVoxelSize ), 1 );

		return numVoxels;
	}

	private static double getSmallestVoxelSize( Source< ? > source )
	{
		final VoxelDimensions voxelDimensions = source.getVoxelDimensions();
		double smallestVoxelSize = Double.MAX_VALUE;
		for ( int d = 0; d < 3; d++ )
		{
			if ( voxelDimensions.dimension( d ) < smallestVoxelSize )
			{
				smallestVoxelSize = voxelDimensions.dimension( d );
			}
		}
		return smallestVoxelSize;
	}
}
