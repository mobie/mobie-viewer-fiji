package de.embl.cba.platynereis.utils;

import bdv.util.*;
import de.embl.cba.platynereis.transforms.Transforms;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.LinAlgHelpers;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class RachelsTransformations
{
	public static void main( String[] args )
	{
		// Euler transformation in Elastix:
		// The parameter vector μ consists of the Euler angles (in rad)
		// and the translation vector.
		// In 3D, this gives a vector of length 6: μ = (θx,θy,θz,tx,ty,tz).
		// The centre of rotation is not part of μ; it is a fixed setting,
		// usually the centre of the image.

		// ProSPr to EM Rachel:
		// translation in micrometer: -64.894, -108.420, 16.081
		// rotation in degrees: 98.6
		// rotation axis: -0.61,-0.47,-0.636

		final Vector3D axis = new Vector3D( -0.61, -0.47, -0.636 );
		double angle = 98.6 / 180.0 * Math.PI;
		double[] translation = new double[]{
				-64.894 * 2,
				-108.42 * 2,
				+16.081 * 2};  // *2 because 2 pixels are 1 micrometer

		final Rotation rotation = new Rotation( axis, angle, RotationConvention.VECTOR_OPERATOR );
		final double[][] matrix = rotation.getMatrix();

		final AffineTransform3D rotationTransform = new AffineTransform3D();

		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++col )
				rotationTransform.set( matrix[ row ][ col ], row, col );


		final ImagePlus musclesProsprImp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration--data/data/prospr-new/muscles.zip" );
		final ImagePlus musclesRachelImp = IJ.openImage( "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/FIB segmentation/muscle.tif" );

		final RandomAccessibleInterval musclesProspr = ImageJFunctions.wrapReal( musclesProsprImp );
		final RandomAccessibleInterval musclesRachel = ImageJFunctions.wrapReal( musclesRachelImp );

		double[] translationOfCenterToOrigin = new double[ 3 ];
		double[] translationOfOriginToCenter = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationOfCenterToOrigin[ d ] = - musclesProspr.dimension( d ) / 2;
			translationOfOriginToCenter[ d ] = - translationOfCenterToOrigin[ d ];
		}

		final AffineTransform3D transform3D = new AffineTransform3D();

		transform3D.translate( translationOfCenterToOrigin );
		transform3D.preConcatenate( rotationTransform );

		final AffineTransform3D transformOriginToCenter = new AffineTransform3D();
		transformOriginToCenter.translate( translationOfOriginToCenter );

		transform3D.preConcatenate( transformOriginToCenter );

		transform3D.translate( translation );


//		final RandomAccessibleInterval musclesProsprTransformed = Transforms.createTransformedView( musclesProspr, transform3D );

		Bdv bdv = BdvFunctions.show( musclesProspr,
				"muscles-prospr-transformed",
				BdvOptions.options().sourceTransform( transform3D )).getBdvHandle();
		final BdvStackSource rachel = BdvFunctions.show( musclesRachel, "rachel",
				BdvOptions.options().addTo( bdv ) );

		rachel.setColor( new ARGBType( ARGBType.rgba( 0,255,0,255 ) ) );

	}
}
