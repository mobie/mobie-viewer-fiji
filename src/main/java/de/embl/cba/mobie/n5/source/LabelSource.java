package de.embl.cba.mobie.n5.source;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.lazyalgorithm.RandomAccessibleIntervalFilter;
import de.embl.cba.lazyalgorithm.converter.NeighborhoodNonZeroBoundariesConverter2;
import de.embl.cba.lazyalgorithm.view.NeighborhoodViews;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * A {@link Source} that wraps another {@link Source} and allows to decorate it
 * with an extra {@link RandomAccessibleIntervalFilter}.
 * <p>
 * This extra transformation is made to capture manual editing of the actual
 * transform in the SpimViewer.
 *
 * @author Christian Tischer - Oct 2020
 *
 * @param <T>
 *            the type of the original source.
 */
public class LabelSource< T extends NumericType< T > & RealType< T > > implements Source< T >
{
	protected final Source< T > source;
	private final DefaultInterpolators< T > interpolators;
	private boolean showAsBoundaries;
	private int boundaryWidth;

	public LabelSource( final Source< T > source )
	{
		this.source = source;
		this.interpolators = new DefaultInterpolators();
	}

	public void showAsBoundary( boolean showAsBoundaries, int boundaryWidth )
	{
		this.showAsBoundaries = showAsBoundaries;
		this.boundaryWidth = boundaryWidth;
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return source.doBoundingBoxCulling();
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		source.getSourceTransform( t, level, transform );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return source.getSource( t, level );

		// below code is not needed, because BDV (I think) always shows the interpolated source
//		RandomAccessibleInterval< T > source = this.source.getSource( t, level );
//
//		if ( showAsBoundaries )
//		{
//			NeighborhoodNonZeroBoundariesConverter2< T > boundariesConverter = new NeighborhoodNonZeroBoundariesConverter2< T >( source );
//			RandomAccessibleInterval boundaries = NeighborhoodViews.neighborhoodConvertedView(
//					source,
//					boundariesConverter,
//					new HyperSphereShape( boundaryWidth ) );
//			return boundaries;
//		}
//		else
//		{
//			return source;
//		}
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		// do not interpolate for label images, but always use NEARESTNEIGHBOR
		if ( showAsBoundaries  )
		{
			RandomAccessibleInterval< T > rai = getSource( t, level );
			RealRandomAccessible< T > interpolate = Views.interpolate( Views.extendZero( rai ), interpolators.get( Interpolation.NEARESTNEIGHBOR ) );
			return interpolate;
		}
		else
		{
			return source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );
		}
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return source.getName();
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return source.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}
}
