/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.transform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.util.Intervals;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.stream.LongStream;

public abstract class TransformConversions
{
	public static AffineTransform3D getAmiraAsPixelUnitsAffineTransform3D(
			double[] amiraRotationAxis,
			double amiraRotationAngleInDegrees,
			double[] amiraTranslationVectorInMicrometer,
			double[] targetImageVoxelSizeInMicrometer,
			double[] targetImageCenterInPixels // this is the center of the rotation
	)
	{

		// rotate
		//
		final Vector3D axis = new Vector3D(
				amiraRotationAxis[ 0 ],
				amiraRotationAxis[ 1 ],
				amiraRotationAxis[ 2 ] );

		double angle = amiraRotationAngleInDegrees / 180.0 * Math.PI;

		final AffineTransform3D rotationTransform = getRotationTransform( axis, angle );

		final AffineTransform3D transform3D =
				getRotationAroundImageCenterTransform( rotationTransform, targetImageCenterInPixels );

		// translate
		//
		double[] translationInPixels = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationInPixels[ d ] =
					amiraTranslationVectorInMicrometer[ d ] / targetImageVoxelSizeInMicrometer[ d ];
		}

		transform3D.translate( translationInPixels );

		return transform3D;
	}

	public static String get_DOESNOTWORK_USE_INVERSE_AmiraAsElastixAffine3D(
			double[] amiraRotationAxis,
			double amiraRotationAngleInDegrees,
			double[] amiraTranslationVectorInMicrometer,
			double targetImageVoxelSizeInMicrometer,
			double[] rotationCentreInPixels ) // this is the center of the rotation)
	{
		// Note: Elastix Spatial Units are millimeters


		// Amira: T(moving) = R(x - cMoving) + cMoving + t
		// Elastix: T(fixed to moving) = R(x - cFixed) + cFixed + t
		// - the translations in the end are in the rotated coordinate system

		// TODO: make also work for anisotropic target image

		String out = "Affine:\n";

		// rotate
		//

		final Vector3D axis = new Vector3D(
				amiraRotationAxis[ 0 ],
				amiraRotationAxis[ 1 ],
				amiraRotationAxis[ 2 ] );


		double angle = amiraRotationAngleInDegrees / 180.0 * Math.PI;

		// Note: Transformation in elastix is defined inverse, i.e. from fixed to moving
		final AffineTransform3D rotationTransform = getRotationTransform( axis, angle ).inverse();

		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++col )
				out += rotationTransform.get( row, col ) + " ";

		// translate
		//

		// Note: Transformation in elastix is defined inverse, i.e. from fixed to moving
		// Note: the given translation is not applied after rotation!

		double[] translationInMillimeters = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationInMillimeters[ d ] = - 1.0 * amiraTranslationVectorInMicrometer[ d ];
			translationInMillimeters[ d ] /= 1000.0; // from micro to millimeter
		}

		rotationTransform.apply( translationInMillimeters, translationInMillimeters );

		for ( int d = 0; d < 3; ++d )
		{
			out += translationInMillimeters[ d ] + " ";
		}

		// centre of rotation
		//

		double[] rotationCentreInMillimeters = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			rotationCentreInMillimeters[ d ] = rotationCentreInPixels[ d ] * targetImageVoxelSizeInMicrometer;
			rotationCentreInMillimeters[ d ] /= 1000.0; // from micro to millimeter
		}

		out += "\nCentre of rotation:\n";
		for ( int d = 0; d < 3; ++d )
			out += rotationCentreInMillimeters[ d ] + " ";

		return out;
	}

	public static AffineTransform3D getRotationAroundImageCenterTransform(
			AffineTransform3D rotationTransform,
			double[] targetImageCenterInPixelUnits )
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

	public static String asStringElastixStyle(
			AffineTransform3D affineTransform3D,
			double voxelSizeInMillimeter)
	{

		String out = "";
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++col )
				out += affineTransform3D.get( row, col ) + " ";


		out += voxelSizeInMillimeter * affineTransform3D.get( 0, 3 ) + " ";
		out += voxelSizeInMillimeter * affineTransform3D.get( 1, 3 ) + " ";
		out += voxelSizeInMillimeter * affineTransform3D.get( 2, 3 );

		return out;
	}

	public static String asStringBdvStyle( AffineTransform3D affineTransform3D )
	{

		String out = "";
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 4; ++col )
				out += String.format( "%.4f",  affineTransform3D.get( row, col ) ) + " ";

		return out;
	}

	public static AffineTransform3D getRotationTransform(
			Vector3D axis,
			double angle )
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

	public static void changeTransformToScaledUnits( AffineTransform3D affineTransform3D, double[] voxelSizeInMicrometer )
	{
		final Scale scale = new Scale( voxelSizeInMicrometer );
		affineTransform3D.concatenate( scale );

		final double[] translation = affineTransform3D.getTranslation();
		for ( int d = 0; d < 3; ++d )
		{
			translation[ d ] *= voxelSizeInMicrometer[ d ];
		}

		affineTransform3D.setTranslation( translation );
	}

	public static AffineTransform3D getElastixSimilarityAsBdvAffine()
	{
		// TODO.
		// (Transform "SimilarityTransform")
		//(NumberOfParameters 7)
		//(TransformParameters -0.008415 0.004752 -0.001727 -0.002337 -0.001490 0.003296 0.987273)
		//(InitialTransformParametersFileName "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/transformations/prospr-to-segmentation/TransformParameters.ManualPreAlignment-Affine_actuallyOnlyEuler.txt")
		//(HowToCombineTransforms "Compose")
		//
		//// Image specific
		//(FixedImageDimension 3)
		//(MovingImageDimension 3)
		//(FixedInternalImagePixelType "float")
		//(MovingInternalImagePixelType "float")
		//(Size 550 518 570)
		//(Index 0 0 0)
		//(Spacing 0.0005000000 0.0005000000 0.0005000000)
		//(Origin 0.0000000000 0.0000000000 0.0000000000)
		//(Direction 1.0000000000 0.0000000000 0.0000000000 0.0000000000 1.0000000000 0.0000000000 0.0000000000 0.0000000000 1.0000000000)
		//(UseDirectionCosines "false")
		//
		//// SimilarityTransform specific
		//(CenterOfRotationPoint 0.0132582577 0.0387138401 0.1074694348)


		return null;
	}

	public static AffineTransform3D getElastixEulerTransformAsAffineTransformInPixelUnits(
			String transform,
			String rotation,
			double[] imageVoxelSizeInMicrometer )
	{
		String[] split = transform.split( " " );

		final double[] angles = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			angles[ d ] = Double.parseDouble( split[ d ] );
		}

		final double[] translationInPixels = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationInPixels[ d ] = Double.parseDouble( split[ d + 3 ] ) / ( 0.001 * imageVoxelSizeInMicrometer[ d ] );
		}


		split = rotation.split( " " );

		final double[] rotationCentreVectorInPixelsPositive = new double[ 3 ];
		final double[] rotationCentreVectorInPixelsNegative = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			rotationCentreVectorInPixelsPositive[ d ] = Double.parseDouble( split[ d  ] ) / ( 0.001 * imageVoxelSizeInMicrometer[ d ] );
			rotationCentreVectorInPixelsNegative[ d ] = - Double.parseDouble( split[ d  ] ) / ( 0.001 * imageVoxelSizeInMicrometer[ d ] );
		}


		final AffineTransform3D transform3D = new AffineTransform3D();

		// rotate around rotation centre
		//
		transform3D.translate( rotationCentreVectorInPixelsNegative ); // + or - ??
		for ( int d = 0; d < 3; ++d)
		{
			transform3D.rotate( d, angles[ d ]);
		}
		final AffineTransform3D translateBackFromRotationCentre = new AffineTransform3D();
		translateBackFromRotationCentre.translate( rotationCentreVectorInPixelsPositive );
		transform3D.preConcatenate( translateBackFromRotationCentre );


		// translate
		//
		transform3D.translate( translationInPixels );

		return transform3D;
	}
}
