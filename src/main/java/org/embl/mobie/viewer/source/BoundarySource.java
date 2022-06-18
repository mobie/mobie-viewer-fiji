/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.source;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.tables.imagesegment.ImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converters;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.segment.SegmentAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public class BoundarySource< T > implements Source< T >, SourceWrapper< T >
{
    private final Source< T > source;
    private boolean showAsBoundaries;
    private float boundaryWidth;
    private ArrayList< Integer > boundaryDimensions;

    public BoundarySource( final Source< T > source )
    {
        this.source = source;
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
    public boolean isPresent( final int t )
    {
        return source.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval< T > getSource( final int t, final int level )
    {
        return source.getSource( t, level );
    }


    @Override
    public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method)
    {
        final RealRandomAccessible< T > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        if ( showAsBoundaries && rra.realRandomAccess().get() instanceof Volatile )
        {
            // Ultimately we need the boundaries
            // in pixel units, because
            // we have to check the voxel values
            // in the rra, which is in voxel units.
            // However, it feels like we could stay
            // longer in physical units here to
            // make this less confusing...

            final float[] boundarySizePixelUnits = getBoundarySize( t, level );

            final FunctionRealRandomAccessible< V > volatileBoundaryRRA = createVolatileBoundaryRRA( ( RealRandomAccessible ) rra, boundaryDimensions, boundarySizePixelUnits );
            return Converters.convert( volatileBoundaryRRA, ( input, output ) -> {
                if ( ! input.isValid() )
                    output.setValid( false );
                else
                    set( t, output, input.get() );
            }, new VolatileSegmentType() );
        }

        return Converters.convert( rra, ( input, output ) -> set( t, output, input ), new VolatileSegmentType() );

    }

    private void set( int t, VolatileSegmentType output, T input )
    {
        output.set( new VolatileSegmentType( adapter.getSegment( input.getRealDouble(), t, source.getName() ) ) );
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

    private FunctionRealRandomAccessible< V > createVolatileBoundaryRRA( RealRandomAccessible< V > rra, ArrayList< Integer > boundaryDimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, V > boundaries = ( l, output ) ->
        {
            final RealRandomAccess< V > access = rra.realRandomAccess();
            V value = ( V ) access.setPositionAndGet( l );
            if ( ! value.isValid() )
            {
                output.setValid( false );
                return;
            }
            final float centerFloat = value.get().getRealFloat();
            if ( centerFloat == background )
            {
                output.get().setReal( background );
                output.setValid( true );
                return;
            }
            for ( Integer d : boundaryDimensions )
            {
                for ( int signum = -1; signum <= +1; signum +=2  ) // back and forth
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    value = access.get();
                    if ( ! value.isValid() )
                    {
                        output.setValid( false );
                        return;
                    }
                    else if ( centerFloat != value.get().getRealFloat() )
                    {
                        output.get().setReal( centerFloat );
                        output.setValid( true );
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            output.get().setReal( background );
            output.setValid( true );
            return;
        };
        final V type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< V > randomAccessible = new FunctionRealRandomAccessible( 3, boundaries, () -> type.get().createVariable() );
        return randomAccessible;
    }

    @Override
    public VolatileSegmentType getType() {
        return new VolatileSegmentType();
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

    @Override
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
