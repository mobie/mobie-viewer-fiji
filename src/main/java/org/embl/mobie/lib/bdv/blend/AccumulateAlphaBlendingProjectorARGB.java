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
package org.embl.mobie.lib.bdv.blend;

import bdv.util.BdvHandle;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AccumulateAlphaBlendingProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	public static BdvHandle bdvHandle;
	public static ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();;

	private final boolean[] alphaBlending;
	private final int[] order;

	public AccumulateAlphaBlendingProjectorARGB(
			final List< VolatileProjector > sourceProjectors,
			final List< SourceAndConverter< ? > > sources,
			final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target )
	{
		super( sourceProjectors, sourceScreenImages, target );
		alphaBlending = getAlphaBlending( sources );
		order = getOrder( sources );
	}

	public static synchronized int[] getOrder( List< SourceAndConverter< ? > > sources )
	{
		final ArrayList< SourceAndConverter< ? > > sorted = new ArrayList<>( sources );

		Collections.sort(sorted, Comparator.comparingLong(sac -> {
			Long timeAdded = (Long) sacService.getMetadata(sac, BlendingMode.TIME_ADDED);
			return timeAdded != null ? timeAdded : Long.MIN_VALUE;
		}));
		int[] order = new int[ sorted.size() ];
		for ( int i = 0; i < order.length; i++)
			order[i] = sources.indexOf( sorted.get(i) );
		return order;
	}

	public static synchronized boolean[] getAlphaBlending( List< SourceAndConverter< ? > > sources )
	{
		final int numSources = sources.size();
		final boolean[] alphaBlending = new boolean[ numSources ];
		final String blendingModeKey = BlendingMode.class.getName();
		for ( int sourceIndex = 0; sourceIndex < numSources; sourceIndex++ )
		{
			final BlendingMode blendingMode = ( BlendingMode ) sacService.getMetadata( sources.get( sourceIndex ), blendingModeKey );
			if ( blendingMode == null || blendingMode.equals( BlendingMode.Alpha ) )
				alphaBlending[ sourceIndex ] = true;
		}
		return alphaBlending;
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses, alphaBlending, order );
		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses, boolean[] alphaBlending, int[] order )
	{
		try
		{
			int rAccu = 0, gAccu = 0, bAccu = 0;

			int numSources = accesses.length;

			for ( int i = 0; i < numSources; i++ )
			{
				int sourceIndex = order[ i ];
				final int argb = accesses[ sourceIndex ].get().get();
				final double alpha = ARGBType.alpha( argb ) / 255.0;
				if ( alpha == 0 ) continue;

				final int r = ARGBType.red( argb );
				final int g = ARGBType.green( argb );
				final int b = ARGBType.blue( argb );

				if ( i > 0 && alphaBlending[ sourceIndex ] )
				{
					rAccu *= ( 1 - alpha );
					gAccu *= ( 1 - alpha );
					bAccu *= ( 1 - alpha );
				}

				if ( i > 0 )
				{
					rAccu += r * alpha;
					gAccu += g * alpha;
					bAccu += b * alpha;
				}
				else
				{
					rAccu += r;
					gAccu += g;
					bAccu += b;
				}
			}

			if ( rAccu > 255 )
				rAccu = 255;
			if ( gAccu > 255 )
				gAccu = 255;
			if ( bAccu > 255 )
				bAccu = 255;

			return ARGBType.rgba( rAccu, gAccu, bAccu, 255 );
		}
		catch ( Exception e )
		{
			throw new RuntimeException();
		}
	}
}
