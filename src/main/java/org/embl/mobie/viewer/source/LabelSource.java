package org.embl.mobie.viewer.source;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class LabelSource<T extends NumericType<T> & RealType<T>> implements Source<T> {
    protected final Source<T> source;
    private boolean showAsBoundaries;
    private float boundaryWidth;
    private float background;
    private final RealMaskRealInterval bounds;
    private ArrayList< Integer > boundaryDimensions;

    public LabelSource( final Source<T> source )
    {
        this( source, 0 );
    }

    public LabelSource( final Source<T> source, float background )
    {
        this( source, background, null );
    }

    public LabelSource( final Source<T> source, float background, RealMaskRealInterval bounds )
    {
        this.source = source;
        this.background = background;
        this.bounds = bounds;
    }

    public void showAsBoundary( boolean showAsBoundaries, float boundaryWidth ) {
        this.showAsBoundaries = showAsBoundaries;
        this.boundaryWidth = boundaryWidth;
        this.boundaryDimensions = boundaryDimensions();
    }

    @Override
    public boolean doBoundingBoxCulling() {
        return source.doBoundingBoxCulling();
    }

    @Override
    public synchronized void getSourceTransform(final int t, final int level, final AffineTransform3D transform) {
        source.getSourceTransform(t, level, transform);
    }

    @Override
    public boolean isPresent(final int t) {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource(final int t, final int level)
    {
       return source.getSource( t, level );
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        if ( showAsBoundaries  )
        {
            // Ultimately we need the boundaries in pixel units, because
            // we have to check the voxel values in the rra, which is in voxel units.
            // However, it feels like we could stay longer in physical units here to
            // make this less confusing...
            final float[] boundarySizePixelUnits = getBoundarySize( t, level );

            if ( rra.realRandomAccess().get() instanceof Volatile )
            {
                return createVolatileBoundaryRRA( rra, boundaryDimensions, boundarySizePixelUnits );
            }
            else
            {
                return createBoundaryRRA( rra, boundaryDimensions, boundarySizePixelUnits );
            }
        }
        else
        {
            return rra;
        }
    }

    private ArrayList< Integer > boundaryDimensions()
    {
        final ArrayList< Integer > dimensions = new ArrayList<>();
        if ( bounds != null )
        {
            // check whether the source is effectively 2D,
            // i.e. much thinner along one dimension than the
            // requested boundary width
            for ( int d = 0; d < 3; d++ )
            {
                final double sourceBound = Math.abs( bounds.realMax( d ) - bounds.realMin( d ) );
                if ( sourceBound > 3 * boundaryWidth )
                    dimensions.add( d );
            }
        }
        else if ( source.getSource( 0,0 ).dimension( 2 ) == 1 )
        {
            // 2D source
            dimensions.add( 0 );
            dimensions.add( 1 );
        }
        else
        {
            // 3D source
            dimensions.add( 0 );
            dimensions.add( 1 );
            dimensions.add( 2 );
        }

        return dimensions;
    }

    private float[] getBoundarySize( int t, int level )
    {
        final float[] boundaries = new float[ 3 ];
        Arrays.fill( boundaries, (float) boundaryWidth );
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        getSourceTransform( t, level, sourceTransform );
        for ( int d = 0; d < 3; d++ )
        {
            final double scale = Affine3DHelpers.extractScale( sourceTransform, d );
            boundaries[ d ] /= scale;
        }
        return boundaries;
    }

    private FunctionRealRandomAccessible< T > createBoundaryRRA( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, T > biConsumer = ( l, o ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            T centerPixel = access.setPositionAndGet( l );
            final float realFloat = centerPixel.getRealFloat();
            if ( realFloat == background )
            {
                o.setReal( background );
                return;
            }
            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    if ( realFloat != access.get().getRealFloat() )
                    {
                        // it is a boundary pixel!
                        o.setReal( realFloat );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            o.setReal( background );
            return;
        };
        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > labelBoundaries = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.copy() );
        return labelBoundaries;
    }

    private FunctionRealRandomAccessible< T > createVolatileBoundaryRRA( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, T > biConsumer = ( l, o ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            Volatile< T > value = ( Volatile< T > ) access.setPositionAndGet( l );
            Volatile< T > vo = ( Volatile< T > ) o;
            if ( ! value.isValid() )
            {
                vo.setValid( false );
                return;
            }
            final float centerFloat = value.get().getRealFloat();
            if ( centerFloat == background )
            {
                vo.get().setReal( background );
                vo.setValid( true );
                return;
            }
            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    value = ( Volatile< T > ) access.get();
                    if ( ! value.isValid() )
                    {
                        vo.setValid( false );
                        return;
                    }
                    else if ( centerFloat != value.get().getRealFloat() )
                    {
                        vo.get().setReal( centerFloat );
                        vo.setValid( true );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            vo.get().setReal( background );
            vo.setValid( true );
            return;
        };
        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.copy() );
        return randomAccessible;
    }

    @Override
    public T getType() {
        return source.getType();
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return source.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return source.getNumMipmapLevels();
    }

    public Source<T> getWrappedSource() {
        return source;
    }

    public boolean isShowAsBoundaries()
    {
        return showAsBoundaries;
    }

    public float getBoundaryWidth()
    {
        return boundaryWidth;
    }
}
