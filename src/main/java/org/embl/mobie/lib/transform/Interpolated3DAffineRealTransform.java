package org.embl.mobie.lib.transform;

import IceInternal.Ex;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Interpolated3DAffineRealTransform implements RealTransform {
    private TreeMap<Double, double[]> transforms = new TreeMap<>();
    private final Map<Double, AffineTransform3D> cache = new ConcurrentHashMap<>();
    private final double precision;

    public Interpolated3DAffineRealTransform( ) {
        this( 1.0 );
    }

    public Interpolated3DAffineRealTransform( double precision ) {
        this.precision = precision;
    }

    public void addTransform( double z, double[] transform ) {
        transforms.put(z, transform);
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
    public void apply(double[] source, double[] target) {
        if (transforms.isEmpty()) throw new IllegalStateException("No transforms added.");
        final AffineTransform3D interpolatedTransform = getInterpolatedTransform( source[2] );
        interpolatedTransform.apply(source, target);
    }

    @Override
    public void apply( RealLocalizable source, RealPositionable target )
    {
        if (transforms.isEmpty()) throw new IllegalStateException("No transforms added.");
        final AffineTransform3D interpolatedTransform = getInterpolatedTransform( source.getDoublePosition(2 ) );
        interpolatedTransform.apply(source, target);
    }

    @Override
    public RealTransform copy() {
        Interpolated3DAffineRealTransform copy = new Interpolated3DAffineRealTransform();
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

    private AffineTransform3D getInterpolatedTransform( double z ) {
        double cacheKey = Math.round(z / precision ) * precision;
        return cache.computeIfAbsent(cacheKey, k -> {
            Entry<Double, double[]> floor = transforms.floorEntry( z );
            Entry<Double, double[]> ceil = transforms.ceilingEntry( z );

            if (floor == null || ceil == null || floor.getKey().equals(ceil.getKey())) {
                AffineTransform3D affineTransform3D = new AffineTransform3D();
                if(floor != null)
                    affineTransform3D.set( floor.getValue() );
                else
                    affineTransform3D.set( ceil.getValue() );
                return affineTransform3D;
            } else {
                double t = ( z - floor.getKey()) / (ceil.getKey() - floor.getKey());
                return interpolateTransforms( floor.getValue(), ceil.getValue(), t );
            }
        });
    }

    private AffineTransform3D interpolateTransforms(double[] matrix1,
                                                    double[] matrix2,
                                                    double t) {
        try
        {
            double[] interpolate = new double[ matrix1.length ];
            for ( int i = 0; i < matrix1.length; i++ ) {
                interpolate[ i ] = matrix1[ i ] * ( 1 - t ) + matrix2[ i ] * t;
            }
            AffineTransform3D interpolatedTransform = new AffineTransform3D();
            interpolatedTransform.set( interpolate );
            return interpolatedTransform;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}

