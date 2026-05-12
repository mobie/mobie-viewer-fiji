package org.embl.mobie.lib.transform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Builds a precomputed inverse displacement field from a forward transform.
 */
public class InverseDisplacementFieldTransformFactory
{
    private final DisplacementFieldTransform displacementFieldTransform;

    public InverseDisplacementFieldTransformFactory(
            final RealTransform forwardTransform,
            double[] origin,
            double[] spacing,
            int[] size )
    {
        int n = spacing.length;
		final long[] fieldDims = new long[ n + 1 ];
		fieldDims[ 0 ] = n;
		for ( int d = 0; d < n; d++ )
			fieldDims[ d + 1 ] = size[ d ];

		final RandomAccessibleInterval< DoubleType > inverseField = ArrayImgs.doubles( fieldDims );
		final RandomAccess< DoubleType > fieldAccess = inverseField.randomAccess();

		final WrappedIterativeInvertibleRealTransform< RealTransform > wrappedInverse =
				new WrappedIterativeInvertibleRealTransform<>( forwardTransform );
		wrappedInverse.getOptimzer().setMaxStep( 500.0 );
		wrappedInverse.getOptimzer().setTolerance( 0.5 );
		wrappedInverse.getOptimzer().setMaxIters( 200 );

		final RealTransform inverse = wrappedInverse.inverse();
		final double[] y = new double[ n ];
		final double[] x = new double[ n ];
		final long[] accessPosition = new long[ n + 1 ];

		final LocalizingIntervalIterator iterator = new LocalizingIntervalIterator( size );
		while ( iterator.hasNext() )
		{
			iterator.fwd();
			for ( int d = 0; d < n; d++ )
				y[ d ] = origin[ d ] + iterator.getLongPosition( d ) * spacing[ d ];

			inverse.apply( y, x );

			for ( int d = 0; d < n; d++ )
			{
				accessPosition[ 0 ] = d;
				for ( int e = 0; e < n; e++ )
					accessPosition[ e + 1 ] = iterator.getLongPosition( e );
				fieldAccess.setPosition( accessPosition );
				fieldAccess.get().set( x[ d ] - y[ d ] );
			}
		}

        displacementFieldTransform = new DisplacementFieldTransform( inverseField, spacing, origin );
    }

    public RealTransform get()
    {
        return displacementFieldTransform;
    }
}
