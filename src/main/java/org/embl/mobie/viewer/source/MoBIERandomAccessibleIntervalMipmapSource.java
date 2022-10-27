package org.embl.mobie.viewer.source;

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

public class MoBIERandomAccessibleIntervalMipmapSource< T extends Type< T > > implements Source< T >
{
	private final List< RandomAccessibleInterval< T > > mipmapSources;
	private final AffineTransform3D[] mipmapTransforms;
	private final VoxelDimensions voxelDimensions;
	private final T type;
	private final String name;
	private final DefaultInterpolators< ? extends NumericType > interpolators;

	public MoBIERandomAccessibleIntervalMipmapSource(
			final List< RandomAccessibleInterval< T > > imgs,
			final T type,
			final VoxelDimensions voxelDimensions,
			final String name,
			AffineTransform3D[] mipmapTransforms )
	{
		this.type = type;
		this.name = name;
		assert imgs.size() == mipmapTransforms.length : "Number of mipmaps and scale factors do not match.";

		this.mipmapSources = imgs;
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
			return Views.interpolate( ra, new NearestNeighborInterpolatorFactory<>() );
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
