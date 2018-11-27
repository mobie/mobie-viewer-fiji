package de.embl.cba.platynereis.transforms;

import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.ArrayList;


public abstract class Transforms< T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{

	public static int[] XYZ = new int[]{0,1,2};


    public static < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
    RealTransform createIdentityAffineTransformation( int numDimensions )
    {
        if ( numDimensions == 2 )
        {
            return (T) new AffineTransform2D();
        }
        else if ( numDimensions == 3 )
        {
            return (T) new AffineTransform3D();
        }
        else
        {
            return (T) new AffineTransform( numDimensions );
        }
    }


    public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform combinedTransform,
													InterpolatorFactory interpolatorFactory)
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, combinedTransform, interpolatorFactory );
		final FinalInterval transformedInterval = createTransformedInterval( rai, combinedTransform );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, transformedInterval );

		return transformedIntervalView;

	}

	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform combinedTransform,
													FinalInterval interval,
													InterpolatorFactory interpolatorFactory)
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, combinedTransform, interpolatorFactory );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, interval );

		return transformedIntervalView;

	}

	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform transform )
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, transform, new ClampingNLinearInterpolatorFactory() );
		final FinalInterval transformedInterval = createTransformedInterval( rai, transform );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, transformedInterval );

		return transformedIntervalView;

	}

	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform transform,
													FinalInterval interval )
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, transform, new NLinearInterpolatorFactory() );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, interval );

		return transformedIntervalView;

	}

	public static < S extends RealType< S > & NativeType< S >, T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > getWithAdjustedOrigin( RandomAccessibleInterval< S > reference,
														 RandomAccessibleInterval< T > target )
	{
		long[] offset = new long[ reference.numDimensions() ];
		reference.min( offset );
		RandomAccessibleInterval translated = Views.translate( target, offset );
		return translated;
	}

	public static < T extends NumericType< T > >
	RandomAccessible createTransformedRaView( RandomAccessibleInterval< T > rai, InvertibleRealTransform combinedTransform, InterpolatorFactory interpolatorFactory )
	{
		RealRandomAccessible rra = Views.interpolate( Views.extendZero( rai ), interpolatorFactory );

		rra = RealViews.transform( rra, combinedTransform );
		return Views.raster( rra );
	}

    public static < T extends NumericType< T > >
	FinalInterval createTransformedInterval( RandomAccessibleInterval< T > rai, InvertibleRealTransform transform )
	{
		final FinalInterval transformedInterval;

		if ( transform instanceof AffineTransform3D )
		{
			FinalRealInterval transformedRealInterval = ( ( AffineTransform3D ) transform ).estimateBounds( rai );
			transformedInterval = toInterval( transformedRealInterval );
		}
		else if ( transform instanceof Scale )
		{
			transformedInterval = createScaledInterval( rai, ( Scale ) transform );
		}
		else
		{
			transformedInterval = null;
		}

		return transformedInterval;
	}

	public static < T extends NumericType< T > >
	FinalInterval createScaledInterval( RandomAccessibleInterval< T > rai, Scale scale )
	{
		long[] min = new long[ rai.numDimensions() ];
		long[] max = new long[ rai.numDimensions() ];
		rai.min( min );
		rai.max( max );

		for ( int d = 0; d < rai.numDimensions(); ++d  )
		{
			min[ d ] *= scale.getScale( d );
			max[ d ] *= scale.getScale( d );
		}

		return new FinalInterval( min, max );
	}

	public static FinalInterval toInterval( FinalRealInterval realInterval )
	{
		double[] realMin = new double[ 3 ];
		double[] realMax = new double[ 3 ];
		realInterval.realMin( realMin );
		realInterval.realMax( realMax );

		long[] min = new long[ 3 ];
		long[] max = new long[ 3 ];

		for ( int d : XYZ )
		{
			min[ d ] = (long) realMin[ d ];
			max[ d ] = (long) realMax[ d ];
		}

		return new FinalInterval( min, max );
	}

	public static AffineTransform3D getTransformToIsotropicRegistrationResolution( double binning, double[] calibration )
	{
		double[] downScaling = new double[ 3 ];

		for ( int d : XYZ )
		{
			downScaling[ d ] = calibration[ d ] / binning;
		}

		final AffineTransform3D scalingTransform = createScalingTransform( downScaling );

		return scalingTransform;
	}


	public static double[] getScalingFactors( double[] calibration, double targetResolution )
	{

		double[] scalings = new double[ calibration.length ];

		for ( int d = 0; d < calibration.length; ++d )
		{
			scalings[ d ] = calibration[ d ] / targetResolution;
		}

		return scalings;
	}

	public static AffineTransform3D getScalingTransform( double[] calibration, double targetResolution )
	{

		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d : XYZ )
		{
			scaling.set( calibration[ d ] / targetResolution, d, d );
		}

		return scaling;
	}

	public static AffineTransform3D getScalingTransform( double calibration, double targetResolution )
	{

		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d : XYZ )
		{
			scaling.set( calibration / targetResolution, d, d );
		}

		return scaling;
	}



	public static AffineTransform3D createScalingTransform( double[] calibration )
	{
		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d : XYZ )
		{
			scaling.set( calibration[ d ], d, d );
		}

		return scaling;
	}

	public static <T extends RealType<T> & NativeType< T > >
	ArrayList< RandomAccessibleInterval< T > > transformAllChannels(
			RandomAccessibleInterval< T > images,
			AffineTransform3D registrationTransform,
			FinalInterval outputImageInterval )
	{
		ArrayList< RandomAccessibleInterval< T > > transformedChannels = new ArrayList<>(  );

		long numChannels = images.dimension( 3 );

		for ( int c = 0; c < numChannels; ++c )
		{
			final RandomAccessibleInterval< T > channel = Views.hyperSlice( images, 3, c );
			transformedChannels.add(  createTransformedView( channel, registrationTransform, outputImageInterval ) );
		}

		return transformedChannels;
	}


	public static ArrayList< RealPoint > origin()
	{
		final ArrayList< RealPoint > origin = new ArrayList<>();
		origin.add( new RealPoint( new double[]{ 0, 0, 0 } ) );
		return origin;
	}
}
