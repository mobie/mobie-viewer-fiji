/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.transform;

import com.google.gson.Gson;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class InterpolatedAffineRealTransform implements RealTransform {
    private final String name;
    private final AffineTransform3D globalToSource;
    private TreeMap<Double, double[]> transforms;
    private transient final Map<Integer, AffineTransform3D> cache;

    public InterpolatedAffineRealTransform( String name, AffineTransform3D globalToSource ) {
        this.name = name;
        // globalToSource is needed to know which z-position in the source corresponds
        // to the given global position in the {@code apply( ... )} method
        // because the z-position in the source is the key in the {@code transforms}
        this.globalToSource = globalToSource;
        transforms = new TreeMap<>();
        cache = new ConcurrentHashMap<>();
    }

    public void addTransform( double z, double[] transform ) {
        transforms.put( z, transform );
    }

    public void addTransforms( TreeMap< Double, double[] > transforms )
    {
        this.transforms.putAll( transforms );
    }
    
    @Override
    public int numSourceDimensions() {
        return 3;
    }

    @Override
    public int numTargetDimensions() {
        return 3;
    }

    /*
    Interpolates the stored transformations along the z coordinate of the source.
     */
    @Override
    public void apply( double[] source, double[] target ) {
        if (transforms.isEmpty()) throw new IllegalStateException("No transforms added.");
        final double[] voxelPositionInSource = getVoxelPositionInSource( source );
        final AffineTransform3D interpolatedTransform = getInterpolatedTransform( voxelPositionInSource[2] );
        interpolatedTransform.apply(source, target);
    }

    @Override
    public void apply( RealLocalizable source, RealPositionable target )
    {
        if (transforms.isEmpty()) throw new IllegalStateException("No transforms added.");
        final double[] voxelPositionInSource = getVoxelPositionInSource( source.positionAsDoubleArray() );
        final AffineTransform3D interpolatedTransform = getInterpolatedTransform( voxelPositionInSource[ 2 ] );
        interpolatedTransform.apply(source, target);
    }

    @NotNull
    private double[] getVoxelPositionInSource( double[] source )
    {
        final double[] voxelPositionInSource = new double[ 3 ];
        globalToSource.apply( source, voxelPositionInSource );
        return voxelPositionInSource;
    }

    @Override
    public RealTransform copy()
    {
        InterpolatedAffineRealTransform copy = new InterpolatedAffineRealTransform( name, globalToSource.copy() );
        for (Entry<Double, double[]> entry : transforms.entrySet())
        {
            copy.addTransform( entry.getKey(), entry.getValue() );
        }

        return copy;
    }

    @Override
    public boolean isIdentity()
    {
        return false;
    }

    private AffineTransform3D getInterpolatedTransform( double z )
    {
        return cache.computeIfAbsent( (int) z, k -> computeInterpolatedAffineTransform3D( z ) );
    }

    @NotNull
    private AffineTransform3D computeInterpolatedAffineTransform3D( double z )
    {
        Entry< Double, double[] > floor = transforms.floorEntry( z );
        Entry< Double, double[] > ceil = transforms.ceilingEntry( z );

        if ( floor == null || ceil == null || floor.getKey().equals( ceil.getKey() ) )
        {
            AffineTransform3D affineTransform3D = new AffineTransform3D();
            if ( floor != null )
                affineTransform3D.set( floor.getValue() );
            else
                affineTransform3D.set( ceil.getValue() );
            return affineTransform3D;
        }
        else
        {
            double t = ( z - floor.getKey() ) / ( ceil.getKey() - floor.getKey() );
            return interpolateTransforms( floor.getValue(), ceil.getValue(), t );
        }
    }

    private AffineTransform3D interpolateTransforms(double[] matrix1,
                                                    double[] matrix2,
                                                    double t) {
            double[] interpolate = new double[ matrix1.length ];
            for ( int i = 0; i < matrix1.length; i++ ) {
                interpolate[ i ] = matrix1[ i ] * ( 1 - t ) + matrix2[ i ] * t;
            }
            AffineTransform3D interpolatedTransform = new AffineTransform3D();
            interpolatedTransform.set( interpolate );
            return interpolatedTransform;
    }

    public String toJSON()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public TreeMap< Double, double[] > getTransforms()
    {
        return transforms;
    }

    public String getName()
    {
        return name;
    }
}

