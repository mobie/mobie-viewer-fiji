package mobie3.viewer.source;


import bdv.viewer.Source;
import mobie3.viewer.transform.RealIntervalProvider;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class AbstractLazySpimSource< N extends NumericType< N > > implements RealIntervalProvider
{
	protected final SourceAndConverterAndTables< N > sourceAndConverterAndTables;

	public AbstractLazySpimSource( SourceAndConverterAndTables< N > sourceAndConverterAndTables )
	{
		this.sourceAndConverterAndTables = sourceAndConverterAndTables;
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
		return sourceAndConverterAndTables.getName();
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
		return sourceAndConverterAndTables.getInitializationSourceAndConverter().getSpimSource();
	}

	protected Source< ? extends Volatile< N > > getVolatileInitializationSource()
	{
		return sourceAndConverterAndTables.getInitializationSourceAndConverter().asVolatile().getSpimSource();
	}

	public SourceAndConverterAndTables< ? > getLazySourceAndConverterAndTables()
	{
		return sourceAndConverterAndTables;
	}

	@Override
	public FinalRealInterval getRealInterval( int t )
	{
		final double[] min = getInitializationSource().getSource( t, 0 ).minAsDoubleArray();
		final double[] max = getInitializationSource().getSource( t, 0 ).maxAsDoubleArray();
		return new FinalRealInterval( min, max ) ;
	}
}

