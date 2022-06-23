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

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

import javax.annotation.Nullable;
import java.util.Map;

public class TransformedTimePointSource<T extends NumericType<T> & RealType<T>> implements Source<T>, SourceWrapper<T>
{
    private final String name;
    private final Source<T> source;
    private Map< Integer, Integer > timePoints; // new to old
    private boolean keep;

    public TransformedTimePointSource( @Nullable String name, final Source< T > source, Map< Integer, Integer > timePoints, boolean keep )
    {
        this.name = name;
        this.source = source;
        this.timePoints = timePoints;
        this.keep = keep;
    }

    @Override
    public boolean doBoundingBoxCulling() {
        return source.doBoundingBoxCulling();
    }

    @Override
    public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
    {
        final int timePointInWrappedSource = getTimePointInWrappedSource( t );
        if ( timePointInWrappedSource == -1 )
        {
            final Integer existingTimePointInWrappedSource = timePoints.entrySet().iterator().next().getValue();
            source.getSourceTransform( existingTimePointInWrappedSource, level, transform);
        }
        else
        {
            source.getSourceTransform( timePointInWrappedSource, level, transform );
        }

    }

    @Override
    public boolean isPresent( final int t )
    {
        if ( timePoints.keySet().contains( t ) )
            return true;
        else if ( keep )
            return source.isPresent( t );
        else
            return false;
    }

    @Override
    public RandomAccessibleInterval<T> getSource(final int t, final int level)
    {
        return source.getSource( getTimePointInWrappedSource( t ), level );
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(final int t, final int level, final Interpolation method)
    {
       return source.getInterpolatedSource( getTimePointInWrappedSource( t ), level, method );
    }

    @Override
    public T getType() {
        return source.getType();
    }

    @Override
    public String getName() {
        if ( name == null )
            return source.getName();
        else
            return name;
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

    private int getTimePointInWrappedSource( int t )
    {
        if ( timePoints.containsKey( t ) )
        {
            return timePoints.get( t );
        }
        else if ( keep )
        {
            // Timepoint is not in the map, but keep=true,
            // therefore use timepoint in input image (aka wrapped source).
            return t;
        }
        else
        {
            // missing t crashes: https://github.com/bigdataviewer/bigdataviewer-core/issues/140
            //System.err.println( "Access at non-existing timepoint: " + t + "; returning t = 0 instead.");
            //Thread.dumpStack();
            return -1;
        }
    }

}
