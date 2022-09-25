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
package org.embl.mobie.viewer.spots;

import net.imglib2.KDTree;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.function.DoubleUnaryOperator;

/**
 * A radial basis function interpolator
 * @author John Bogovic
 *
 * @param <T>
 */
public class RBFInterpolator< T extends NumericType<T> > extends RealPoint implements RealRandomAccess< T >
{
	final static protected double minThreshold = Double.MIN_VALUE * 1000;

	final protected RadiusNeighborSearch< T > search;

	final protected KDTree< T > tree;

	final T type;

	double searchRadius;

	final DoubleUnaryOperator intensityComputer;  // from squaredDistance to weight

	final boolean normalize;

	public RBFInterpolator(
			final KDTree< T > tree,
			final DoubleUnaryOperator intensityComputer,
			final double searchRadius,
			final boolean normalize,
			T type )
	{
		super( tree.numDimensions() );

		this.intensityComputer = intensityComputer;
		this.tree = tree;
		this.search = new RadiusNeighborSearchOnKDTree< T >( tree );
		this.normalize = normalize;
		this.searchRadius = searchRadius;

		this.type = type;
	}
	
	public void setRadius( final double radius )
	{
		this.searchRadius = radius;
	}

	public void increaseRadius( final double amount )
	{
		this.searchRadius += amount;
	}

	public void decreaseRadius( final double amount )
	{
		this.searchRadius -= amount;
	}

	@Override
	public T get()
	{
		search.search( this, searchRadius, true );

		T copy = type.copy();

		AffineTransform3D affineTransform3D = new AffineTransform3D();

		RealPoint dataPoint = new RealPoint( 3 );
		affineTransform3D.apply( this, dataPoint );

		if ( search.numNeighbors() > 0 )
		{
			final Sampler< T > sampler = search.getSampler( 0 );
			final T value = sampler.get();
			final double weight = intensityComputer.applyAsDouble( search.getSquareDistance( 0 ) );
			copy.set( value );
			copy.mul( weight );
			return copy;
		}
		else
		{
			copy.setZero();
			return copy;
		}
	}

	@Override
	public RBFInterpolator< T > copy()
	{
		return new RBFInterpolator< T >( tree, intensityComputer, searchRadius, normalize, type );
	}

	@Override
	public RBFInterpolator< T > copyRealRandomAccess()
	{
		return copy();
	}

	public static class RBFInterpolatorFactory< T extends NumericType<T> > implements InterpolatorFactory< T, KDTree< T > >
	{
		final double searchRad;
		final DoubleUnaryOperator intensityComputer;
		final boolean normalize;
		T type;

		public RBFInterpolatorFactory( 
				final DoubleUnaryOperator intensityComputer,
				final double sr, 
				final boolean normalize, 
				T type )
		{
			this.searchRad = sr;
			this.intensityComputer = intensityComputer;
			this.normalize = normalize;
			this.type = type;
		}

		@Override
		public RBFInterpolator<T> create( final KDTree< T > tree )
		{
			return new RBFInterpolator<T>( tree, intensityComputer, searchRad, normalize, type );
		}

		@Override
		public RealRandomAccess< T > create(
				final KDTree< T > tree,
				final RealInterval interval )
		{
			return create( tree );
		}
	}
}
