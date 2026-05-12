package develop;

import itc.converters.ElastixBSplineToBSplineRealTransform;
import itc.transforms.elastix.ElastixTransform;
import net.imglib2.realtransform.RealTransform;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class DevelopInspectElastixBSplineEffect
{
	private static final String DEFAULT_TRANSFORM_PATH =
			"/Users/tischer/Desktop/hitt2t/Tischi-Mobie-Bspline/TransformParameters.2.txt";

	private static class Sample
	{
		final double[] source;
		final double[] displacement;
		final double magnitude;

		Sample( final double[] source, final double[] displacement, final double magnitude )
		{
			this.source = source;
			this.displacement = displacement;
			this.magnitude = magnitude;
		}
	}

	public static void main( final String[] args ) throws Exception
	{
		final String transformPath = args.length > 0 ? args[ 0 ] : DEFAULT_TRANSFORM_PATH;
		final int samplesPerDim = args.length > 1 ? Integer.parseInt( args[ 1 ] ) : 20;
		final double threshold = args.length > 2 ? Double.parseDouble( args[ 2 ] ) : 1e-6;

		final File transformFile = new File( transformPath );
		if ( !transformFile.exists() )
			throw new IllegalArgumentException( "Transform file not found: " + transformFile.getAbsolutePath() );

		final ElastixTransform elastixTransform = ElastixTransform.load( transformFile );
		final RealTransform transform = ElastixBSplineToBSplineRealTransform.convert( elastixTransform );

		final int n = transform.numSourceDimensions();
		if ( n < 2 || n > 3 )
			throw new UnsupportedOperationException( "Only 2D/3D BSpline supported here, got: " + n + "D" );

		System.out.println( "Transform: " + transformPath );
		System.out.println( "Dimensions: " + n );
		System.out.println( "Samples per dimension: " + samplesPerDim );
		System.out.println( "Change threshold (physical units): " + threshold );
		System.out.println();

		final double[] minCoord = new double[ n ];
		final double[] maxCoord = new double[ n ];
		for ( int d = 0; d < n; d++ )
		{
			minCoord[ d ] = elastixTransform.Origin[ d ];
			maxCoord[ d ] = elastixTransform.Origin[ d ] +
					( elastixTransform.Size[ d ] - 1 ) * elastixTransform.Spacing[ d ];
		}

		System.out.println( "Sampling bounds (from Origin/Size/Spacing):" );
		System.out.println( "  min = " + Arrays.toString( minCoord ) );
		System.out.println( "  max = " + Arrays.toString( maxCoord ) );
		System.out.println();

		final Stats stats = new Stats( n, threshold );
		sampleGridRecursive( transform, minCoord, maxCoord, samplesPerDim, 0, new int[ n ], stats );

		stats.print();
	}

	private static class Stats
	{
		final int n;
		final double threshold;
		long total = 0;
		long changed = 0;
		final double[] minDisp;
		final double[] maxDisp;
		double maxMagnitude = Double.NEGATIVE_INFINITY;
		final double[] maxMagnitudePoint;
		final double[] changedMinCoord;
		final double[] changedMaxCoord;
		final PriorityQueue< Sample > top5;

		Stats( final int n, final double threshold )
		{
			this.n = n;
			this.threshold = threshold;
			this.minDisp = fill( n, Double.POSITIVE_INFINITY );
			this.maxDisp = fill( n, Double.NEGATIVE_INFINITY );
			this.maxMagnitudePoint = new double[ n ];
			this.changedMinCoord = fill( n, Double.POSITIVE_INFINITY );
			this.changedMaxCoord = fill( n, Double.NEGATIVE_INFINITY );
			this.top5 = new PriorityQueue<>( 5, Comparator.comparingDouble( s -> s.magnitude ) );
		}

		private static double[] fill( final int n, final double v )
		{
			final double[] a = new double[ n ];
			Arrays.fill( a, v );
			return a;
		}

		void add( final double[] source, final double[] target )
		{
			total++;
			final double[] disp = new double[ n ];
			double sq = 0.0;
			for ( int d = 0; d < n; d++ )
			{
				disp[ d ] = target[ d ] - source[ d ];
				if ( disp[ d ] < minDisp[ d ] ) minDisp[ d ] = disp[ d ];
				if ( disp[ d ] > maxDisp[ d ] ) maxDisp[ d ] = disp[ d ];
				sq += disp[ d ] * disp[ d ];
			}
			final double mag = Math.sqrt( sq );

			if ( mag > maxMagnitude )
			{
				maxMagnitude = mag;
				System.arraycopy( source, 0, maxMagnitudePoint, 0, n );
			}

			if ( mag > threshold )
			{
				changed++;
				for ( int d = 0; d < n; d++ )
				{
					if ( source[ d ] < changedMinCoord[ d ] ) changedMinCoord[ d ] = source[ d ];
					if ( source[ d ] > changedMaxCoord[ d ] ) changedMaxCoord[ d ] = source[ d ];
				}

				top5.add( new Sample( source.clone(), disp, mag ) );
				if ( top5.size() > 5 ) top5.poll();
			}
		}

		void print()
		{
			System.out.println( "Global displacement ranges (target - source):" );
			for ( int d = 0; d < n; d++ )
				System.out.println( "  d" + d + ": [" + minDisp[ d ] + ", " + maxDisp[ d ] + "]" );

			System.out.println();
			System.out.println( "Max displacement magnitude: " + maxMagnitude );
			System.out.println( "  at source: " + Arrays.toString( maxMagnitudePoint ) );

			System.out.println();
			System.out.println( "Changed samples (> threshold): " + changed + " / " + total );
			if ( changed > 0 )
			{
				System.out.println( "Approx. source-region where transform does something:" );
				System.out.println( "  min = " + Arrays.toString( changedMinCoord ) );
				System.out.println( "  max = " + Arrays.toString( changedMaxCoord ) );
			}
			else
			{
				System.out.println( "No sampled point exceeded threshold. Try denser sampling or lower threshold." );
			}

			System.out.println();
			System.out.println( "Top changed sample points:" );
			final Sample[] top = top5.toArray( new Sample[ 0 ] );
			Arrays.sort( top, ( a, b ) -> Double.compare( b.magnitude, a.magnitude ) );
			for ( int i = 0; i < top.length; i++ )
			{
				System.out.println(
					"  #" + ( i + 1 ) +
					" |source=" + Arrays.toString( top[ i ].source ) +
					" |disp=" + Arrays.toString( top[ i ].displacement ) +
					" |mag=" + top[ i ].magnitude );
			}
		}
	}

	private static void sampleGridRecursive(
			final RealTransform transform,
			final double[] minCoord,
			final double[] maxCoord,
			final int samplesPerDim,
			final int dim,
			final int[] index,
			final Stats stats )
	{
		if ( dim == index.length )
		{
			final double[] source = new double[ index.length ];
			final double[] target = new double[ index.length ];
			for ( int d = 0; d < index.length; d++ )
			{
				final double t = samplesPerDim == 1 ? 0.0 : (double) index[ d ] / ( samplesPerDim - 1 );
				source[ d ] = minCoord[ d ] + t * ( maxCoord[ d ] - minCoord[ d ] );
			}
			transform.apply( source, target );
			stats.add( source, target );
			return;
		}

		for ( int i = 0; i < samplesPerDim; i++ )
		{
			index[ dim ] = i;
			sampleGridRecursive( transform, minCoord, maxCoord, samplesPerDim, dim + 1, index, stats );
		}
	}
}

