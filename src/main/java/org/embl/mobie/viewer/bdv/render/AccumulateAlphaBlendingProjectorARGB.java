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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AccumulateAlphaBlendingProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	private final ArrayList< Boolean > isAlphaBlending;

	public AccumulateAlphaBlendingProjectorARGB(
			final List< VolatileProjector > sourceProjectors,
			final List< SourceAndConverter< ? > > sources,
			final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads, // TODO
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target );
		isAlphaBlending = getIsAlphaBlending( sources );
		System.out.println( Arrays.toString( sources.toArray( new String[ 0 ] )));
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses, isAlphaBlending );
		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses, ArrayList< Boolean > alphaBlending )
	{
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		int[] argbs = getARGBs( accesses );

		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			final int argb = argbs[ sourceIndex ];
			final double alpha = ARGBType.alpha( argb ) / 255.0;
			if ( alpha == 0 ) continue;

			final int r = ARGBType.red( argb );
			final int g = ARGBType.green( argb );
			final int b = ARGBType.blue( argb );

			try
			{
				if ( alphaBlending.get( sourceIndex ) )
				{
					rAccu *= ( 1 - alpha );
					gAccu *= ( 1 - alpha );
					bAccu *= ( 1 - alpha );
				}
			}
			catch ( Exception e )
			{
				throw new RuntimeException();
			}

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

	private static int[] getARGBs( Cursor< ? extends ARGBType >[] accesses )
	{
		int[] argbs = new int[ accesses.length ];
		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			argbs[ sourceIndex ] = accesses[ sourceIndex ].get().get();
		}
		return argbs;
	}

	public static ArrayList< Boolean > getIsAlphaBlending( List< SourceAndConverter< ? > > sources )
	{
		final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

		final ArrayList< Boolean > alphaBlending = new ArrayList<>();
		for ( SourceAndConverter< ? > source : sources )
		{
			final BlendingMode blendingMode = ( BlendingMode ) sacService.getMetadata( source, BlendingMode.BLENDING_MODE );
			if ( blendingMode != null && blendingMode.equals( BlendingMode.Alpha ) )
				alphaBlending.add( true );
			else
				alphaBlending.add( false );
		}
		return alphaBlending;
	}
}
