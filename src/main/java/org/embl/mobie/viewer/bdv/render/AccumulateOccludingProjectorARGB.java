/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package org.embl.mobie.viewer.bdv.render;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AccumulateOccludingProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	private static BlendingMode[] blendingModes;
	private static ArrayList< ArrayList< Integer > > occlusions;

	public AccumulateOccludingProjectorARGB(
			final List< VolatileProjector > sourceProjectors,
			final List< SourceAndConverter< ? > > sources,
			final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
		blendingModes = getBlendingModes( sources );
		occlusions = initOcclusions( blendingModes );
	}

	public static ArrayList< ArrayList< Integer > > getOcclusions( List< SourceAndConverter< ? > > sacs )
	{
		final BlendingMode[] blendingModes = getBlendingModes( sacs );
		final ArrayList< ArrayList< Integer > > occlusions = initOcclusions( blendingModes );
		return occlusions;
	}

	public static BlendingMode[] getBlendingModes( List< SourceAndConverter< ? > > sources )
	{
		final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

		return sources.stream()
				.map(sac -> sacService.getMetadata( sac, BlendingMode.BLENDING_MODE ) )
				.map(it -> it==null ? BlendingMode.Sum:it)
				.toArray( BlendingMode[]::new );
	}

	private static ArrayList< ArrayList< Integer > > initOcclusions( BlendingMode[] blendingModes )
	{
		ArrayList< ArrayList< Integer > > occludedBy = new ArrayList();

		for ( int sourceIndex = 0; sourceIndex < blendingModes.length; sourceIndex++ )
		{
			final ArrayList< Integer > occludingSubsequentSourceIndices = new ArrayList<>();
			occludedBy.add( occludingSubsequentSourceIndices );

			for ( int subsequentSourceIndex = sourceIndex + 1; subsequentSourceIndex < blendingModes.length; subsequentSourceIndex++ )
			{
				if ( BlendingMode.isOccluding( blendingModes[ subsequentSourceIndex ] ) )
				{
					occludingSubsequentSourceIndices.add( subsequentSourceIndex );
				}
			}
		}

		return occludedBy;
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses, occlusions );
		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses, ArrayList< ArrayList< Integer > > occludedBy )
	{
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		int[] argbs = getARGBs( accesses );

		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			final int argb = argbs[ sourceIndex ];
			final int a = ARGBType.alpha( argb );
			if ( a == 0 ) continue;
			if ( isOccluded( argbs, occludedBy.get( sourceIndex ) ) ) continue;

			final int r = ARGBType.red( argb );
			final int g = ARGBType.green( argb );
			final int b = ARGBType.blue( argb );

			final double alpha = a / 255.0;
			aAccu += a; // does this make sense??
			rAccu += r * alpha;
			gAccu += g * alpha;
			bAccu += b * alpha;
		}

		if ( aAccu > 255 )
			aAccu = 255;
		if ( rAccu > 255 )
			rAccu = 255;
		if ( gAccu > 255 )
			gAccu = 255;
		if ( bAccu > 255 )
			bAccu = 255;

		return ARGBType.rgba( rAccu, gAccu, bAccu, aAccu );
	}

	private static boolean isOccluded( int[] argbs, ArrayList< Integer > occlusions )
	{
		for ( Integer occludingSourceIndex : occlusions )
		{
			if ( ARGBType.alpha( argbs[ occludingSourceIndex ] ) > 0 )
			{
				return true;
			}
		}
		return false;
	}

	private static int[] getARGBs( Cursor< ? extends ARGBType >[] accesses )
	{
		int[] argbs = new int[ accesses.length ];
		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			argbs[ sourceIndex ] = accesses[ sourceIndex ].get().get();
		}
		return argbs;
	}
}
