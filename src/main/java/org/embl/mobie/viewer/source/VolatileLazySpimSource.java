package org.embl.mobie.viewer.source;


import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazySpimSource< N extends NumericType< N >, V extends Volatile< N > > implements Source< V >
{
	private final LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables;

	public VolatileLazySpimSource( LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables )
	{
		this.lazySourceAndConverterAndTables = lazySourceAndConverterAndTables;
	}

	@Override
	public boolean isPresent( int t )
	{
		return getSpimSource().isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< V > getSource( int t, int level )
	{
		return getSpimSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< V > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return getSpimSource().getInterpolatedSource( t, level, method );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		getSpimSource().getSourceTransform( t, level, transform );
	}

	@Override
	public V getType()
	{
		return getSpimSource().getType();
	}

	@Override
	public String getName()
	{
		return lazySourceAndConverterAndTables.getName();
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return lazySourceAndConverterAndTables.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return getSpimSource().getNumMipmapLevels();
	}

	public double[] getMin()
	{
		return lazySourceAndConverterAndTables.getMin();
	}

	public double[] getMax()
	{
		return lazySourceAndConverterAndTables.getMax();
	}

	private Source< V > getSpimSource()
	{
		return (Source< V >) lazySourceAndConverterAndTables.openSourceAndConverter().asVolatile().getSpimSource();
	}

	public LazySourceAndConverterAndTables< N > getLazySourceAndConverterAndTables()
	{
		return lazySourceAndConverterAndTables;
	}
}

