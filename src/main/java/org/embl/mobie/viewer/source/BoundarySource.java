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
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.segment.SegmentAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public class BoundarySource< T extends AnnotationType< T > > extends AbstractBoundarySource< T >
{
    private final Source< T > source;
    private boolean showAsBoundaries;
    private float boundaryWidth;
    private ArrayList< Integer > boundaryDimensions;

    public BoundarySource( final Source< T > source )
    {
        this.source = source;
    }

    @Override
    public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
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


    private FunctionRealRandomAccessible< T > createBoundaryRRA( RealRandomAccessible< T > rra, ArrayList< Integer > dimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, T > biConsumer = ( l, o ) ->
        {
            final RealRandomAccess< T > access = rra.realRandomAccess();
            T centerValue = access.setPositionAndGet( l );
            if ( centerValue.getAnnotation() == null )
            {
                o.set( null );
                return;
            }
            for ( Integer d : dimensions )
            {
                for ( int signum = -1; signum <= +1; signum+=2 ) // forth and back
                {
                    access.move( signum * boundaryWidth[ d ], d );
                    if ( ! centerValue.valueEquals( access.get() ) )
                    {
                        o.set( centerValue ); // it is a boundary pixel!
                        return;
                    }
                    access.move( - signum * boundaryWidth[ d ], d ); // move back to center
                }
            }
            o.set( null ); // no boundary pixel
            return;
        };
        final T type = rra.realRandomAccess().get();
        final FunctionRealRandomAccessible< T > labelBoundaries = new FunctionRealRandomAccessible( 3, biConsumer, () -> type.copy() );
        return labelBoundaries;
    }

}
