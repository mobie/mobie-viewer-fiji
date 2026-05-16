package org.embl.mobie.lib.transform;

import java.util.Arrays;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Temporary local copy of ITC precomputed inverse displacement-field support.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class InverseDisplacementFieldTransformCreator
{
  public interface ProgressListener
  {
    void onProgress( int percent );
  }

  private static final double DEFAULT_OPTIMIZER_MAX_STEP = 500.0;
  private static final double DEFAULT_OPTIMIZER_TOLERANCE = 0.5;
  private static final int DEFAULT_OPTIMIZER_MAX_ITERS = 200;

  private final RealTransform forwardTransform;
  private final double[] sourceDomainMin;
  private final double[] sourceDomainMax;
  private final double[] inverseSamplingSpacing;
  private final double optimizerMaxStep;
  private final double optimizerTolerance;
  private final int optimizerMaxIterations;

  private SampledInverseDisplacement sampled;

  public InverseDisplacementFieldTransformCreator(
      final RealTransform forwardTransform,
      final double[] sourceDomainMin,
      final double[] sourceDomainMax,
      final double[] inverseSamplingSpacing )
  {
    this(
        forwardTransform,
        sourceDomainMin,
        sourceDomainMax,
        inverseSamplingSpacing,
        DEFAULT_OPTIMIZER_MAX_STEP,
        DEFAULT_OPTIMIZER_TOLERANCE,
        DEFAULT_OPTIMIZER_MAX_ITERS
    );
  }

  public InverseDisplacementFieldTransformCreator(
      final RealTransform forwardTransform,
      final double[] sourceDomainMin,
      final double[] sourceDomainMax,
      final double[] inverseSamplingSpacing,
      final double optimizerMaxStep,
      final double optimizerTolerance,
      final int optimizerMaxIterations )
  {
    this.forwardTransform = forwardTransform;
    this.sourceDomainMin = Arrays.copyOf( sourceDomainMin, sourceDomainMin.length );
    this.sourceDomainMax = Arrays.copyOf( sourceDomainMax, sourceDomainMax.length );
    this.inverseSamplingSpacing = Arrays.copyOf( inverseSamplingSpacing, inverseSamplingSpacing.length );
    this.optimizerMaxStep = optimizerMaxStep;
    this.optimizerTolerance = optimizerTolerance;
    this.optimizerMaxIterations = optimizerMaxIterations;

    validateDimensions();
  }

  public DisplacementFieldTransform get()
  {
    final SampledInverseDisplacement s = sampleInverseDisplacement();
    return new DisplacementFieldTransform( s.interleavedField, s.spacing, s.min );
  }

  public SampledInverseDisplacement sampleInverseDisplacement()
  {
    return sampleInverseDisplacement( null );
  }

  public SampledInverseDisplacement sampleInverseDisplacement( final ProgressListener progressListener )
  {
    if ( sampled != null )
      return sampled;

    final int n = forwardTransform.numSourceDimensions();
    final int[] sourceSamplingSize = samplingSize( sourceDomainMin, sourceDomainMax, inverseSamplingSpacing );
    final long totalSourceSamples = numberOfSamples( sourceSamplingSize );
    final Domain inverseDomain = estimateInverseDomain( sourceSamplingSize );
    final int[] inverseSamplingSize = samplingSize( inverseDomain.min, inverseDomain.max, inverseSamplingSpacing );
    final long totalInverseSamples = numberOfSamples( inverseSamplingSize );
    final long totalSamples = Math.max( 1L, totalSourceSamples + totalInverseSamples );
    final int[] lastReportedPercent = new int[] { -1 };
    reportProgress( progressListener, lastReportedPercent, 0 );

    final long[] fieldDimensions = new long[ n + 1 ];
    fieldDimensions[ 0 ] = n;
    for ( int d = 0; d < n; d++ )
      fieldDimensions[ d + 1 ] = inverseSamplingSize[ d ];

    final RandomAccessibleInterval< DoubleType > interleaved = ArrayImgs.doubles( fieldDimensions );
    final RandomAccess< DoubleType > access = interleaved.randomAccess();

    final WrappedIterativeInvertibleRealTransform< RealTransform > invertible =
        new WrappedIterativeInvertibleRealTransform<>( forwardTransform );
    invertible.getOptimzer().setMaxStep( optimizerMaxStep );
    invertible.getOptimzer().setTolerance( optimizerTolerance );
    invertible.getOptimzer().setMaxIters( optimizerMaxIterations );
    final RealTransform inverse = invertible.inverse();

    final double[] inversePhysical = new double[ n ];
    final double[] forwardPhysical = new double[ n ];
    final long[] idx = new long[ n + 1 ];
    final LocalizingIntervalIterator iterator = new LocalizingIntervalIterator( inverseSamplingSize );
    long processedInverseSamples = 0L;
    while ( iterator.hasNext() )
    {
      iterator.fwd();
      for ( int d = 0; d < n; d++ )
        inversePhysical[ d ] = inverseDomain.min[ d ] + iterator.getLongPosition( d ) * inverseSamplingSpacing[ d ];

      inverse.apply( inversePhysical, forwardPhysical );
      for ( int d = 0; d < n; d++ )
      {
        idx[ 0 ] = d;
        for ( int e = 0; e < n; e++ )
          idx[ e + 1 ] = iterator.getLongPosition( e );
        access.setPosition( idx );
        access.get().set( forwardPhysical[ d ] - inversePhysical[ d ] );
      }

      processedInverseSamples++;
      final int percent = ( int ) Math.min( 99L, ( totalSourceSamples + processedInverseSamples ) * 100L / totalSamples );
      reportProgress( progressListener, lastReportedPercent, percent );
    }

    sampled = new SampledInverseDisplacement(
        interleaved,
        Arrays.copyOf( inverseDomain.min, inverseDomain.min.length ),
        Arrays.copyOf( inverseSamplingSpacing, inverseSamplingSpacing.length ),
        Arrays.copyOf( inverseSamplingSize, inverseSamplingSize.length )
    );
    reportProgress( progressListener, lastReportedPercent, 100 );
    return sampled;
  }

  private void validateDimensions()
  {
    final int n = forwardTransform.numSourceDimensions();
    if ( n != forwardTransform.numTargetDimensions() )
      throw new IllegalArgumentException( "Forward transform must have equal source and target dimensions" );
    if ( sourceDomainMin.length != n || sourceDomainMax.length != n || inverseSamplingSpacing.length != n )
      throw new IllegalArgumentException( "Domain bounds and spacing must match transform dimensionality" );
    for ( int d = 0; d < n; d++ )
    {
      if ( sourceDomainMax[ d ] < sourceDomainMin[ d ] )
        throw new IllegalArgumentException( "max must be >= min at dimension " + d );
      if ( inverseSamplingSpacing[ d ] <= 0.0 )
        throw new IllegalArgumentException( "spacing must be > 0 at dimension " + d );
    }
  }

  private static int[] samplingSize( final double[] min, final double[] max, final double[] spacing )
  {
    final int[] size = new int[ min.length ];
    for ( int d = 0; d < min.length; d++ )
    {
      final double span = max[ d ] - min[ d ];
      size[ d ] = Math.max( 1, ( int ) Math.ceil( span / spacing[ d ] ) + 1 );
    }
    return size;
  }

  private Domain estimateInverseDomain( final int[] sourceSamplingSize )
  {
    final int n = forwardTransform.numSourceDimensions();
    final double[] min = new double[ n ];
    final double[] max = new double[ n ];
    Arrays.fill( min, Double.POSITIVE_INFINITY );
    Arrays.fill( max, Double.NEGATIVE_INFINITY );

    final double[] sourcePosition = new double[ n ];
    final double[] mappedPosition = new double[ n ];
    final LocalizingIntervalIterator iterator = new LocalizingIntervalIterator( sourceSamplingSize );
    while ( iterator.hasNext() )
    {
      iterator.fwd();
      for ( int d = 0; d < n; d++ )
        sourcePosition[ d ] = sourceDomainMin[ d ] + iterator.getLongPosition( d ) * inverseSamplingSpacing[ d ];

      forwardTransform.apply( sourcePosition, mappedPosition );
      for ( int d = 0; d < n; d++ )
      {
        if ( mappedPosition[ d ] < min[ d ] )
          min[ d ] = mappedPosition[ d ];
        if ( mappedPosition[ d ] > max[ d ] )
          max[ d ] = mappedPosition[ d ];
      }
    }

    return new Domain( min, max );
  }

  private static long numberOfSamples( final int[] size )
  {
    long samples = 1L;
    for ( final int s : size )
      samples *= s;
    return samples;
  }

  private static void reportProgress(
      final ProgressListener listener,
      final int[] lastReportedPercent,
      final int percent )
  {
    if ( listener == null )
      return;
    if ( percent <= lastReportedPercent[ 0 ] )
      return;
    lastReportedPercent[ 0 ] = percent;
    listener.onProgress( percent );
  }

  public static final class SampledInverseDisplacement
  {
    public final RandomAccessibleInterval< DoubleType > interleavedField;
    public final double[] min;
    public final double[] spacing;
    public final int[] size;

    private SampledInverseDisplacement(
        final RandomAccessibleInterval< DoubleType > interleavedField,
        final double[] min,
        final double[] spacing,
        final int[] size )
    {
      this.interleavedField = interleavedField;
      this.min = min;
      this.spacing = spacing;
      this.size = size;
    }
  }

  private static final class Domain
  {
    private final double[] min;
    private final double[] max;

    private Domain( final double[] min, final double[] max )
    {
      this.min = min;
      this.max = max;
    }
  }
}
