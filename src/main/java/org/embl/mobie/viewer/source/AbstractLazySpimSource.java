package org.embl.mobie.viewer.source;


import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class AbstractLazySpimSource< N extends NumericType< N > >
{
	protected final LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables;

	public AbstractLazySpimSource( LazySourceAndConverterAndTables< N > lazySourceAndConverterAndTables )
	{
		this.lazySourceAndConverterAndTables = lazySourceAndConverterAndTables;
	}

	public boolean isPresent( int t )
	{
		return getInitializationSource().isPresent( t );
	}

	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		getInitializationSource().getSourceTransform( t, level, transform );
	}

	public String getName()
	{
		return lazySourceAndConverterAndTables.getName();
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return getInitializationSource().getVoxelDimensions();
	}

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

	protected Source< N > getInitializationSource()
	{
		return lazySourceAndConverterAndTables.getInitializationSourceAndConverter().getSpimSource();
	}

	protected Source< N > openSpimSource()
	{
		return lazySourceAndConverterAndTables.openSourceAndConverter().getSpimSource();
	}

	public LazySourceAndConverterAndTables< ? > getLazySourceAndConverterAndTables()
	{
		return lazySourceAndConverterAndTables;
	}
}

