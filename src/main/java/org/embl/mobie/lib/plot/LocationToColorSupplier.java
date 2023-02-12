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

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.color.ColoringModel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class LocationToColorSupplier< T > implements Supplier< BiConsumer< RealPoint, ARGBType > >
{
	private final KDTree< T > kdTree;
	private final ColoringModel< T > coloringModel;
	protected final double dotSize;
	private final double aspectRatio;
	private final int background;
	private final BdvHandle bdvHandle;

	public LocationToColorSupplier( KDTree kdTree, ColoringModel coloringModel, final double dotSize, double aspectRatio, int background, BdvHandle bdvHandle )
	{
		this.kdTree = kdTree;
		this.coloringModel = coloringModel;
		this.dotSize = dotSize;
		this.aspectRatio = aspectRatio;
		this.background = background;
		this.bdvHandle = bdvHandle;
	}

	@Override
	public BiConsumer< RealPoint, ARGBType > get()
	{
		return new LocationToColor();
	}

	class LocationToColor implements BiConsumer< RealPoint, ARGBType >
	{
		private final WithinDistancesSearchOnKDTree< T > search;
		private AffineTransform3D viewerTransform;
		private double[] searchDistances;

		public LocationToColor( )
		{
			search = new WithinDistancesSearchOnKDTree<>( kdTree );
			viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
			searchDistances = new double[ 2 ];
			searchDistances[ 0 ] = Affine3DHelpers.extractScale( viewerTransform.inverse(), 0 );
			searchDistances[ 0 ] *= dotSize;
			searchDistances[ 1 ] = searchDistances[ 0 ] * aspectRatio;

			// System.out.println( "" + searchDistances[ 0 ] + ", " + searchDistances[ 1 ]  );
		}

		@Override
		public void accept( RealPoint realPoint, ARGBType argbType )
		{
			search.search( realPoint, searchDistances, false );

			if ( search.numNeighbors() > 0 )
			{
				coloringModel.convert( search.getSampler( 0 ).get(), argbType );

				// The coloring model uses the alpha value to adjust the brightness.
				// Since the default renderer in BDV ignores
				// this we multiply the rgb values accordingly
				argbType.mul( ARGBType.alpha( argbType.get() ) / 255.0 );
			}
			else
			{
				argbType.set( background );
			}
		}
	}
}
