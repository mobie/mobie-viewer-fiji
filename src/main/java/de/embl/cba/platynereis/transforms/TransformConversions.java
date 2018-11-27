package de.embl.cba.platynereis.transforms;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.stream.LongStream;

public abstract class TransformConversions
{
	public static AffineTransform3D getAmiraAsAffineTransform3D(
			double[] amiraRotationAxis,
			double amiraRotationAngleInDegrees,
			double[] amiraTranslationVectorInMicrometer,
			double targetImageVoxelSizeInMicrometer,
			double[] targetImageCenterInPixelUnits // this is the center of the rotation
	)
	{

		// TODO: make also work for anisotropic target image

		// rotate
		//

		final Vector3D axis = new Vector3D(
				amiraRotationAxis[ 0 ],
				amiraRotationAxis[ 1 ],
				amiraRotationAxis[ 2 ] );

		double angle = amiraRotationAngleInDegrees / 180.0 * Math.PI;

		final AffineTransform3D rotationTransform = getRotationTransform( axis, angle );

		final AffineTransform3D transform3D = getRotationAroundImageCenterTransform( rotationTransform, targetImageCenterInPixelUnits );

		// translate
		//

		double[] translation = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translation[ d ] = amiraTranslationVectorInMicrometer[ d ] / targetImageVoxelSizeInMicrometer;
		}

		transform3D.translate( translation );

		return transform3D;
	}

	public static AffineTransform3D getRotationAroundImageCenterTransform( AffineTransform3D rotationTransform, double[] targetImageCenterInPixelUnits )
	{
		double[] translationFromCenterToOrigin = new double[ 3 ];
		double[] translationFromOriginToCenter = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationFromCenterToOrigin[ d ] = - targetImageCenterInPixelUnits[ d ];
			translationFromOriginToCenter[ d ] = + targetImageCenterInPixelUnits[ d ];
		}

		final AffineTransform3D transform3D = new AffineTransform3D();
		transform3D.translate( translationFromCenterToOrigin );
		transform3D.preConcatenate( rotationTransform );
		final AffineTransform3D transformOriginToCenter = new AffineTransform3D();
		transformOriginToCenter.translate( translationFromOriginToCenter );
		transform3D.preConcatenate( transformOriginToCenter );
		return transform3D;
	}

	public static AffineTransform3D getRotationTransform( Vector3D axis, double angle )
	{
		final Rotation rotation = new Rotation( axis, angle, RotationConvention.VECTOR_OPERATOR );
		final double[][] matrix = rotation.getMatrix();

		final AffineTransform3D rotationTransform = new AffineTransform3D();
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++col )
				rotationTransform.set( matrix[ row ][ col ], row, col );
		return rotationTransform;
	}

	public static double[] getImageCentreInPixelUnits( RandomAccessibleInterval musclesProspr )
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( musclesProspr );
		return LongStream.of( dimensions ).mapToDouble( l -> l / 2.0 ).toArray();
	}
}
