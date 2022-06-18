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
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class VolatileBoundarySource< V extends VolatileAnnotationType< V > > extends AbstractBoundarySource< V >
{
    private boolean showAsBoundaries;
    private float boundaryWidth;
    private ArrayList< Integer > boundaryDimensions;

    public VolatileBoundarySource( final Source< V > source )
    {
        super( source );
    }

    @Override
    public RealRandomAccessible< V > getInterpolatedSource( final int t, final int level, final Interpolation method )
    {
        final RealRandomAccessible< V > rra = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

        if ( showAsBoundaries  )
        {
            // Ultimately we need the boundaries in pixel units, because
            // we have to check the voxel values in the rra, which is in voxel units.
            // However, it feels like we could stay longer in physical units here to
            // make this less confusing...
            final float[] boundarySizePixelUnits = getBoundarySize( t, level );

            return createVolatileBoundaryRRA( rra, boundaryDimensions, boundarySizePixelUnits );
        }
        else
        {
            return rra;
        }
    }

    private FunctionRealRandomAccessible< V > createVolatileBoundaryRRA( RealRandomAccessible< V > rra, ArrayList< Integer > boundaryDimensions, float[] boundaryWidth )
    {
        BiConsumer< RealLocalizable, V > boundaries = ( l, output ) ->
        {
            final RealRandomAccess< V > access = rra.realRandomAccess();
            V input = access.setPositionAndGet( l );
            if ( ! input.isValid() )
            {
                output.setValid( false );
                return;
            }
            final V centerValue = input.get();
            if ( centerValue.getAnnotation() == null  )
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
                    input = ( Volatile< V > ) access.get();
                    if ( ! input.isValid() )
                    {
                        output.setValid( false );
                        return;
                    }
                    else if ( centerFloat != centerValue.getRealFloat() )
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
        final FunctionRealRandomAccessible< V > randomAccessible = new FunctionRealRandomAccessible( 3, boundaries, () -> type.copy() );
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
    public Source< V > getWrappedSource() {
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
