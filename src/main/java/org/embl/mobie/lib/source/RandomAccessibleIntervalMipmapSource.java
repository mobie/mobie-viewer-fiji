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
package org.embl.mobie.lib.source;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.List;

public class RandomAccessibleIntervalMipmapSource< T extends Type< T > > implements Source< T >
{
	private final List< RandomAccessibleInterval< T > > mipmapSources;
	private final AffineTransform3D[] mipmapTransforms;
	private final VoxelDimensions voxelDimensions;
	private final T type;
	private final String name;
	private final DefaultInterpolators< ? extends NumericType > interpolators;

	public RandomAccessibleIntervalMipmapSource(
			final List< RandomAccessibleInterval< T > > rais,
			final T type,
			final VoxelDimensions voxelDimensions,
			final String name,
			AffineTransform3D[] mipmapTransforms )
	{
		this.type = type;
		this.name = name;
		assert rais.size() == mipmapTransforms.length : "Number of mipmaps and scale factors do not match.";

		this.mipmapSources = rais;
		this.mipmapTransforms = mipmapTransforms;
		interpolators = new DefaultInterpolators<>();
		this.voxelDimensions = voxelDimensions;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return mipmapSources.get( level );
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( mipmapTransforms[ level ] );
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return mipmapSources.size();
	}


	@Override
	public boolean isPresent( int t )
	{
		return t == 0; // TODO
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return true;
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
	{
		if ( type instanceof NumericType )
		{
			final RandomAccessible ra = Views.extendZero( (RandomAccessibleInterval ) getSource( t, level ) );
			return ( RealRandomAccessible< T > ) Views.interpolate( ra, interpolators.get( method ) );
		}
		else
		{
			final T outOfBoundsVariable = type.createVariable();
			final RandomAccessible ra = new ExtendedRandomAccessibleInterval<>( getSource( t, level ), new OutOfBoundsConstantValueFactory<>( outOfBoundsVariable ) );
			return Views.interpolate( ra, new NearestNeighborInterpolatorFactory< T >() );
		}
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
