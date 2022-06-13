package org.embl.mobie.viewer.source;


import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class LazySpimSource< N extends NumericType< N > > implements Source< N >
{
	private final LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables;

	public LazySpimSource( LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables )
	{
		this.lazySourceAndConverterAndTables = lazySourceAndConverterAndTables;
	}

	@Override
	public boolean isPresent( int t )
	{
		return getSource().isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< N > getSource( int t, int level )
	{
		return getSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< N > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return getSource().getInterpolatedSource( t, level, method );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		getSource().getSourceTransform( t, level, transform );
	}

	@Override
	public N getType()
	{
		return getSource().getType();
	}

	@Override
	public String getName()
	{
		return lazySourceAndConverterAndTables.getName();
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return getInitializationSource().getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return getInitializationSource().getNumMipmapLevels();
	}

	public double[] getMin()
	{
		return getInitializationSource().getSource( 0 ,0  ).minAsDoubleArray();
	}

	public double[] getMax()
	{
		return getInitializationSource().getSource( 0 ,0  ).maxAsDoubleArray();
	}

	private Source< N > getSource()
	{
		return lazySourceAndConverterAndTables.openSourceAndConverter().getSpimSource();
	}

	private Source< ? > getInitializationSource()
	{
		return lazySourceAndConverterAndTables.getInitializationSourceAndConverter().getSpimSource();
	}

	public LazySourceAndConverterAndTables< N > getLazySourceAndConverterAndTables()
	{
		return lazySourceAndConverterAndTables;
	}
}

