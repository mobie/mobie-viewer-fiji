package org.embl.mobie.lib.transform;

import ij.IJ;
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
            final double[] min,
            final double[] max,
            final double[] spacing )
    {
        final int n = spacing.length;
        if ( min.length != n || max.length != n )
            throw new IllegalArgumentException( "min/max/spacing must have same dimensionality." );

        final int[] sourceSamplingSize = new int[ n ];
        for ( int d = 0; d < n; d++ )
        {
            if ( spacing[ d ] <= 0.0 )
                throw new IllegalArgumentException( "Spacing must be > 0 for dimension " + d );
            if ( max[ d ] < min[ d ] )
                throw new IllegalArgumentException( "max must be >= min for dimension " + d );

            final double span = max[ d ] - min[ d ];
            sourceSamplingSize[ d ] = Math.max( 1, (int) Math.ceil( span / spacing[ d ] ) + 1 );
        }

        IJ.log( "Source domain min=" + java.util.Arrays.toString( min ) +
                ", max=" + java.util.Arrays.toString( max ) +
                ", spacing=" + java.util.Arrays.toString( spacing ) +
                ", size=" + java.util.Arrays.toString( sourceSamplingSize ) );

        // Pass 1: estimate inverse-domain bbox by forwarding the whole source-domain sampling grid.
        final double[] inverseDomainMin = new double[ n ];
        final double[] inverseDomainMax = new double[ n ];
        for ( int d = 0; d < n; d++ )
        {
            inverseDomainMin[ d ] = Double.POSITIVE_INFINITY;
            inverseDomainMax[ d ] = Double.NEGATIVE_INFINITY;
        }

        final double[] sourceDomainPhysicalPosition = new double[ n ];
        final double[] forwardMappedPhysicalPosition = new double[ n ];
        final LocalizingIntervalIterator forwardIterator = new LocalizingIntervalIterator( sourceSamplingSize );
        while ( forwardIterator.hasNext() )
        {
            forwardIterator.fwd();
            for ( int d = 0; d < n; d++ )
                sourceDomainPhysicalPosition[ d ] = min[ d ] + forwardIterator.getLongPosition( d ) * spacing[ d ];

            forwardTransform.apply( sourceDomainPhysicalPosition, forwardMappedPhysicalPosition );
            for ( int d = 0; d < n; d++ )
            {
                if ( forwardMappedPhysicalPosition[ d ] < inverseDomainMin[ d ] )
                    inverseDomainMin[ d ] = forwardMappedPhysicalPosition[ d ];
                if ( forwardMappedPhysicalPosition[ d ] > inverseDomainMax[ d ] )
                    inverseDomainMax[ d ] = forwardMappedPhysicalPosition[ d ];
            }
        }

        final int[] inverseSamplingSize = new int[ n ];
        for ( int d = 0; d < n; d++ )
        {
            final double span = inverseDomainMax[ d ] - inverseDomainMin[ d ];
            inverseSamplingSize[ d ] = Math.max( 1, (int) Math.ceil( span / spacing[ d ] ) + 1 );
        }

        IJ.log( "Estimated inverse-domain min=" + java.util.Arrays.toString( inverseDomainMin ) +
                ", max=" + java.util.Arrays.toString( inverseDomainMax ) +
                ", spacing=" + java.util.Arrays.toString( spacing ) +
                ", size=" + java.util.Arrays.toString( inverseSamplingSize ) );

		final long[] fieldDims = new long[ n + 1 ];
		fieldDims[ 0 ] = n;
		for ( int d = 0; d < n; d++ )
			fieldDims[ d + 1 ] = inverseSamplingSize[ d ];

		final RandomAccessibleInterval< DoubleType > inverseField = ArrayImgs.doubles( fieldDims );
		final RandomAccess< DoubleType > fieldAccess = inverseField.randomAccess();

		final WrappedIterativeInvertibleRealTransform< RealTransform > invertibleTransform =
				new WrappedIterativeInvertibleRealTransform<>( forwardTransform );
		invertibleTransform.getOptimzer().setMaxStep( 500.0 );
		invertibleTransform.getOptimzer().setTolerance( 0.5 );
		invertibleTransform.getOptimzer().setMaxIters( 200 );

		final RealTransform inverse = invertibleTransform.inverse();
		final double[] inverseDomainPhysicalPosition = new double[ n ];
		final double[] forwardDomainPhysicalPosition = new double[ n ];
		final long[] displacementFieldIndex = new long[ n + 1 ];

		final LocalizingIntervalIterator iterator = new LocalizingIntervalIterator( inverseSamplingSize );
		while ( iterator.hasNext() )
		{
			iterator.fwd();
			for ( int d = 0; d < n; d++ )
				inverseDomainPhysicalPosition[ d ] = inverseDomainMin[ d ] + iterator.getLongPosition( d ) * spacing[ d ];

            // solve for forward-domain point whose forward transform lands on inverse-domain sample
			inverse.apply( inverseDomainPhysicalPosition, forwardDomainPhysicalPosition );

			for ( int d = 0; d < n; d++ )
			{
				displacementFieldIndex[ 0 ] = d;
				for ( int e = 0; e < n; e++ )
					displacementFieldIndex[ e + 1 ] = iterator.getLongPosition( e );
				fieldAccess.setPosition( displacementFieldIndex );
				fieldAccess.get().set( forwardDomainPhysicalPosition[ d ] - inverseDomainPhysicalPosition[ d ] );
			}
		}

        displacementFieldTransform = new DisplacementFieldTransform( inverseField, spacing, inverseDomainMin );
    }

    public RealTransform get()
    {
        return displacementFieldTransform;
    }
}
