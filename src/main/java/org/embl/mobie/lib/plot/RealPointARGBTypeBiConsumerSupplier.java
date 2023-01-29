/*-
 * #%L
 * Various Java code for ImageJ
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
package org.embl.mobie.lib.plot;

import org.embl.mobie.lib.color.ColoringModel;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.Sampler;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RealPointARGBTypeBiConsumerSupplier< T > implements Supplier< BiConsumer< RealPoint, ARGBType > >
{
	private final KDTree< T > kdTree;
	private final ColoringModel< T > coloringModel;
	private final double radius;
	private final int background;

	public RealPointARGBTypeBiConsumerSupplier( KDTree< T > kdTree, ColoringModel< T > coloringModel, final double radius, int background )
	{
		this.kdTree = kdTree;
		this.coloringModel = coloringModel;
		this.radius = radius;
		this.background = background;
	}

	@Override
	public BiConsumer< RealPoint, ARGBType > get()
	{
		return new RealPointARGBTypeBiConsumer( kdTree, coloringModel, radius );
	}

	class RealPointARGBTypeBiConsumer implements BiConsumer< RealPoint, ARGBType >
	{
		private final RadiusNeighborSearchOnKDTree< T > search;
		private final ColoringModel< T > coloringModel;
		private final double radius;

		public RealPointARGBTypeBiConsumer( KDTree< T > kdTree, ColoringModel< T > coloringModel, double radius )
		{
			search = new RadiusNeighborSearchOnKDTree<>( kdTree );
			this.coloringModel = coloringModel;
			this.radius = radius;
		}

		@Override
		public void accept( RealPoint realPoint, ARGBType argbType )
		{
			search.search( realPoint, radius, true );

			if ( search.numNeighbors() > 0 )
			{
				final Sampler< T > sampler = search.getSampler( 0 );
				final T tableRow = sampler.get();
				coloringModel.convert( tableRow, argbType );

				// The coloring model uses the alpha value to adjust the brightness.
				// Since the default renderer in BDV ignores this we multiply the rgb values accordingly
				final int alpha = ARGBType.alpha( argbType.get() );
				if( alpha < 255 )
					argbType.mul( alpha / 255.0 );
			}
			else
			{
				argbType.set( background );
			}
		}
	}
}
