package develop;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import itc.converters.ElastixBSplineToBSplineRealTransform;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.ImageDataImage;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.ElastixBSplineTransformation;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.transform.ImageTransformer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * Headless local debug harness for the HITT2T dapi_bspline transform chain.
 *
 * This is intentionally not a CI test because it depends on local files.
 */
public class DevelopOpenHitt2TDapiBsplineCollection
{
	private static final String ROOT = "/Users/tischer/Desktop/hitt2t/Tischi-Mobie-Bspline";
	private static final String IMAGE_FILE = ROOT + "/fixed_reference.tif";
	private static final String BSPLINE_FILE = ROOT + "/TransformParameters.2.noInitial.txt";

	// Row-packed 3x4 affine from the collection table.
	private static final double[] AFFINE = new double[]{
			0.707639, 0.435465, 0.403558, 1084.023230,
			-0.385775, 0.832976, -0.374350, 2156.763812,
			-0.305606, 0.086688, 1.131996, 932.963335
	};

	public static void main( String[] args )
	{
		final File imageFile = requireExistingFile( IMAGE_FILE );
		final File bsplineFile = requireExistingFile( BSPLINE_FILE );

		final Image< ? > baseImage = createBaseImage( imageFile );
		final Image< ? > affineImage = applyAffine( baseImage );
		final Image< ? > transformedImage = applyElastixBSpline( affineImage, bsplineFile );

		System.out.println( "Constructed transformed image headlessly." );
		System.out.println( "Base image:        " + baseImage.getName() );
		System.out.println( "After affine:      " + affineImage.getName() );
		System.out.println( "After bspline:     " + transformedImage.getName() );
		System.out.println();

		final double[][] points = new double[][]{
				{ 3034.0, 3679.0, 758.0 },
				{ 1400.0, 2400.0, 1000.0 },
				{ 1600.0, 2600.0, 1100.0 }
		};

		System.out.println( "Global position -> base value, transformed value, delta" );
		for ( double[] p : points )
		{
			final double baseValue = sampleRealAtGlobalPosition( baseImage.getSourcePair().getSource(), p );
			final double transformedValue = sampleRealAtGlobalPosition( transformedImage.getSourcePair().getSource(), p );
			System.out.println(
					Arrays.toString( p ) + " -> " +
					baseValue + ", " + transformedValue + ", " + ( transformedValue - baseValue ) );
		}

		//final double[] global = new double[]{ 3034.0, 3679.0, 758.0 };
		//final double[] global = new double[]{ 2724, 3310, 628.0 };
		final double[] global = new double[]{ 2876, 2736, 514.0 };


		final AffineTransform3D affineWorldFromLocal = new AffineTransform3D();
		affineImage.getSourcePair().getSource().getSourceTransform( 0, 0, affineWorldFromLocal );

		final double[] affineLocal = worldToLocal( affineWorldFromLocal, global );
		final double affineValue = sampleAtLocalPosition( affineImage.getSourcePair().getSource(), affineLocal );

		final RealTransform bspline = extractBsplineTransform( transformedImage.getSourcePair().getSource(), bsplineFile );
		final double[] worldAfterBspline = new double[ 3 ];
		bspline.apply( global, worldAfterBspline );
		final double[] bsplineLocal = worldToLocal( affineWorldFromLocal, worldAfterBspline );
		final double transformedValue = sampleAtLocalPosition( transformedImage.getSourcePair().getSource(), bsplineLocal );

		System.out.println( "Probe global coordinate: " + Arrays.toString( global ) );
		System.out.println( "Affine-only local sampling position:      " + Arrays.toString( affineLocal ) );
		System.out.println( "Affine+BSpline world after BSpline:      " + Arrays.toString( worldAfterBspline ) );
		System.out.println( "Affine+BSpline local sampling position:  " + Arrays.toString( bsplineLocal ) );
		System.out.println( "Affine-only sampled value:               " + affineValue );
		System.out.println( "Affine+BSpline sampled value:            " + transformedValue );
		System.out.println( "Value delta:                             " + ( transformedValue - affineValue ) );
	}

	private static Image< ? > createBaseImage( final File imageFile )
	{
		final ImageDataFormat format = ImageDataFormat.fromPath( imageFile.getAbsolutePath() );
		return new ImageDataImage<>(
				format,
				imageFile.getAbsolutePath(),
				0,
				"fixed_reference",
				null,
				null );
	}

	private static Image< ? > applyAffine( final Image< ? > image )
	{
		final AffineTransformation affineTransformation = new AffineTransformation(
				"Affine",
				AFFINE,
				Collections.singletonList( image.getName() ),
				Collections.singletonList( image.getName() + "_affine" ) );
		return ImageTransformer.affineTransform( image, affineTransformation );
	}

	private static Image< ? > applyElastixBSpline( final Image< ? > image, final File bsplineFile )
	{
		final ElastixBSplineTransformation transformation = new ElastixBSplineTransformation(
				"ElastixBSpline",
				bsplineFile.getAbsolutePath(),
				Collections.singletonList( image.getName() ),
				Collections.singletonList( image.getName() + "_bspline" ) );
		return ImageTransformer.elastixBSplineTransform( image, transformation, true );
	}

	private static double[] worldToLocal( final AffineTransform3D worldFromLocal, final double[] world )
	{
		final double[] local = new double[ world.length ];
		worldFromLocal.inverse().apply( world, local );
		return local;
	}

	private static double sampleAtLocalPosition( final Source< ? > source, final double[] localPosition )
	{
		final RealRandomAccess< ? > access = source
				.getInterpolatedSource( 0, 0, Interpolation.NEARESTNEIGHBOR )
				.realRandomAccess();
		access.setPosition( localPosition );
		return ( ( RealType< ? > ) access.get() ).getRealDouble();
	}

	private static RealTransform extractBsplineTransform( final Source< ? > source, final File bsplineFile )
	{
		final Source< ? > wrapped = ( ( TransformedSource< ? > ) source ).getWrappedSource();
		if ( wrapped instanceof RealTransformedSource )
			return ( ( RealTransformedSource< ? > ) wrapped ).getRealTransform();
		else
			throw new RuntimeException("No real transform found!");
	}

	private static double sampleRealAtGlobalPosition( final Source< ? > source, final double[] globalPosition )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );

		final double[] sourceLocalPosition = worldToLocal( sourceTransform, globalPosition );

		return sampleAtLocalPosition( source, sourceLocalPosition );
	}

	private static File requireExistingFile( final String path )
	{
		final File file = new File( path );
		if ( !file.exists() )
			throw new IllegalArgumentException( "Required file not found: " + file.getAbsolutePath() );
		return file;
	}
}
