package users.sultan;

import de.embl.cba.transforms.utils.TransformConversions;
import ij.IJ;
import ij.ImagePlus;
import itc.converters.AffineTransform3DToElastixAffine3D;
import itc.converters.AmiraEulerToAffineTransform3D;
import itc.transforms.elastix.ElastixAffineTransform3D;
import itc.transforms.elastix.ElastixTransform;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;

public class ConvertXRayAmiraEulerTransformToElastixAffineTransform
{
	public static void main( String[] args )
	{
		final ImagePlus xRayImp = IJ.openImage( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.tif" );

		double[] translationInMicrometer = new double[]{ -54.6, 2.59, 53.39 };
		double[] rotationAxis = new double[]{ -0.27, 0.81, 0.51 };
		double rotationAngleDegrees = 71.7;

		final double[] imageVoxelSizeInMicrometer = { 1.0, 1.0, 1.0 };

		final double[] rotationCentreInMicrometer = new double[ 3 ];
		rotationCentreInMicrometer[ 0 ] = xRayImp.getWidth() / 2.0 * imageVoxelSizeInMicrometer[ 0 ];
		rotationCentreInMicrometer[ 1 ] = xRayImp.getHeight() / 2.0 * imageVoxelSizeInMicrometer[ 1 ];
		rotationCentreInMicrometer[ 2 ] = xRayImp.getNSlices() / 2.0 * imageVoxelSizeInMicrometer[ 1 ];

		final AffineTransform3D affineTransform3DInMillimeter =
				AmiraEulerToAffineTransform3D.convert(
						rotationAxis,
						rotationAngleDegrees,
						translationInMicrometer,
						rotationCentreInMicrometer );


		/**
		 * This must be the dimensions of the image that we would like to produce
		 * with transformix, e.g. the dimensions of the image that we would like to match,
		 * using this transform as an initial transform, here this is this one:
		 * /Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.tif
		 *
		 */

		final AffineTransform3DToElastixAffine3D affineTransform3DToElastixAffine3D
				= new AffineTransform3DToElastixAffine3D(
				ElastixTransform.FINAL_LINEAR_INTERPOLATOR,
				ElastixTransform.RESULT_IMAGE_PIXEL_TYPE_UNSIGNED_CHAR,
				new Double[]{ 0.0004, 0.0004, 0.0004 },
				new Integer[]{ 687, 648, 713 }
		);

		// invert, because elastix transform goes from output to input
		final AffineTransform3D inverse = affineTransform3DInMillimeter.inverse();

		final ElastixAffineTransform3D elastixAffineTransform3D
				= affineTransform3DToElastixAffine3D.convert( inverse );

		elastixAffineTransform3D.save( "/Users/tischer/Desktop/elastix-affine-transform.txt" );



	}
}
