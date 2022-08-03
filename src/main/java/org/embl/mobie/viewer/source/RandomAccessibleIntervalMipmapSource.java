package org.embl.mobie.viewer.source;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolator;
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
			final List< RandomAccessibleInterval< T > > imgs,
			final T type,
			final double[][] mipmapScales,
			final VoxelDimensions voxelDimensions,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this.type = type;
		this.name = name;
		assert imgs.size() == mipmapScales.length : "Number of mipmaps and scale factors do not match.";

		this.mipmapSources = imgs;
		this.mipmapTransforms = new AffineTransform3D[ mipmapScales.length ];
		for ( int s = 0; s < mipmapScales.length; ++s )
		{
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			mipmapTransform.set(
					mipmapScales[ s ][ 0 ], 0, 0, 0.5 * ( mipmapScales[ s ][ 0 ] - 1 ),
					0, mipmapScales[ s ][ 1 ], 0, 0.5 * ( mipmapScales[ s ][ 1 ] - 1 ),
					0, 0, mipmapScales[ s ][ 2 ], 0.5 * ( mipmapScales[ s ][ 2 ] - 1 ) );
			mipmapTransform.preConcatenate(sourceTransform);
			mipmapTransforms[ s ] = mipmapTransform;
		}
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
		final T outOfBoundsVariable = type.createVariable();
		final RandomAccessible ra = new ExtendedRandomAccessibleInterval<>( getSource( t, level ), new OutOfBoundsConstantValueFactory<>( outOfBoundsVariable ) );
		if ( type instanceof NumericType )
			return ( RealRandomAccessible<T> ) Views.interpolate( ra, interpolators.get( method ) );
		return Views.interpolate( ra, new NearestNeighborInterpolatorFactory<>() );
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
