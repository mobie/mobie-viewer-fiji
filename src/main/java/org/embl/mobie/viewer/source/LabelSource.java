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
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

import java.util.Arrays;
import java.util.function.BiConsumer;

public class LabelSource<T extends NumericType<T> & RealType<T>> implements Source<T> {
    protected final Source<T> source;
    private boolean showAsBoundaries;
    private double boundaryWidth;

    private float background = 0;

    public LabelSource( final Source<T> source )
    {
        this.source = source;
    }

    public LabelSource( final Source<T> source, float background )
    {
        this.source = source;
        this.background = background;
    }

    public void showAsBoundary( boolean showAsBoundaries, double boundaryWidth ) {
        this.showAsBoundaries = showAsBoundaries;
        this.boundaryWidth = boundaryWidth;
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
            final int numDimensions = getWrappedSource().getSource( 0, 0 ).dimension( 2 ) == 1 ? 2 : 3;

            final float[] boundaries = getBoundaries( t, level );

            if ( rra.realRandomAccess().get() instanceof Volatile )
            {
                return createVolatileBoundaryRRA( rra, numDimensions, boundaries );
            }
            else
            {
                return createBoundaryRRA( rra, numDimensions, boundaries );
            }
        }
        else
        {
            return rra;
        }
    }

    private float[] getBoundaries( int t, int level )
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

    private FunctionRealRandomAccessible< T > createBoundaryRRA( RealRandomAccessible< T > rra, int nD, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, T > biConsumer = ( l, o ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            T value = access.setPositionAndGet( l );
            final float centerFloat = value.getRealFloat();
            if ( centerFloat == background )
            {
                o.setReal( background );
                return;
            }
            for ( int d = 0; d < nD; d++ ) // dimensions
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    value = access.get();
                    if ( centerFloat != value.getRealFloat() )
                    {
                        // it is a boundary pixel
                        o.setReal( centerFloat );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            o.setReal( background );
            return;
        };
        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.copy() );
        return randomAccessible;
    }

    private FunctionRealRandomAccessible< T > createVolatileBoundaryRRA( RealRandomAccessible< T > rra, int nD, float[] boundaryWidth )
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
            for ( int d = 0; d < nD; d++ ) // dimensions
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
}
