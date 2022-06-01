/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package users.rachel;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.transforms.utils.TransformConversions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.ARGBType;

public class SegmentedParapodiumMusclesToSegmentedBaselMuscles
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static void main( String[] args )
	{
		final ImagePlus musclesParaImp = IJ.openImage( "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/FIB segmentation/muscle.tif" );

		musclesParaImp.show();

		final ImagePlus musclesEMBLImp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration--data/data/em-segmented/em-segmented-muscles.tif" );
		final ImagePlus musclesBaselImp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration--data/data/em-segmented/em-segmented-muscles-ariadne-500nm.tif" );

		final RandomAccessibleInterval musclesEMBL = ImageJFunctions.wrapReal( musclesEMBLImp );
		final RandomAccessibleInterval musclesBasel = ImageJFunctions.wrapReal( musclesBaselImp );
		final RandomAccessibleInterval musclesParapodium = ImageJFunctions.wrapReal( musclesParaImp );

		// SegmentedParapodium to SegmentedBasel from AMIRA
		double[] translationInMicrometer = new double[]{ 147.9, 48.13, 103.0661 };
		double[] rotationAxis = new double[]{ 0.064, 0.762, 0.643 };
		double rotationAngle = 237.0;

		final double[] imageVoxelSizeInMicrometer = { 0.5, 0.5, 0.5 };

		// new
		final AffineTransform3D affineTransform3D =
				TransformConversions.getAmiraAsPixelUnitsAffineTransform3D(
					rotationAxis,
					rotationAngle,
					translationInMicrometer,
					imageVoxelSizeInMicrometer,
					TransformConversions.getImageCentreInPixelUnits( musclesParapodium ) );


		Bdv bdv = BdvFunctions.show( musclesParapodium,
				"muscles-para-transformed",
				BdvOptions.options().sourceTransform( affineTransform3D )).getBdvHandle();

		final BdvStackSource ariadne = BdvFunctions.show( musclesBasel, "muscles-ariadane",
				BdvOptions.options().addTo( bdv ) );
		ariadne.setColor( new ARGBType( ARGBType.rgba( 0,255,0,255 ) ) );

		final BdvStackSource embl = BdvFunctions.show( musclesEMBL, "muscles-embl",
				BdvOptions.options().addTo( bdv ) );
		embl.setColor( new ARGBType( ARGBType.rgba( 255,255,0,255 ) ) );


		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );


		//
		// Generate elastix transform for improvements
		//

		System.out.println( "\nElastixAffineTransformation" );
		System.out.println( TransformConversions.asStringElastixStyle( affineTransform3D.inverse() , 0.0005 ) );


		//
		// Optimise previous transform using Euler using elastix,
		//

		String transform = "-0.025923 0.010507 -0.002698 -0.002372 -0.001931 0.005464";
		String centerOfRotation = "0.0132582577 0.0387138401 0.1074694348";

		TransformConversions.getElastixEulerTransformAsAffineTransformInPixelUnits(
				transform,
				centerOfRotation,
				imageVoxelSizeInMicrometer
				);



		//
		// Generate Bdv transform for 10x10x10nm full data set
		//

		double voxelSizeInMicrometer = 0.01;

		final Scale scale = new Scale(
				new double[]{
						voxelSizeInMicrometer,
						voxelSizeInMicrometer,
						voxelSizeInMicrometer } );

		double[] paraFullResImageCentreInMicrometer = new double[]{
				4560 / 2.0 * voxelSizeInMicrometer,
				4008 / 2.0 * voxelSizeInMicrometer,
				7246 / 2.0 * voxelSizeInMicrometer};

		final AffineTransform3D affineTransform3DForBdv =
				TransformConversions.getAmiraAsPixelUnitsAffineTransform3D(
					rotationAxis,
					rotationAngle,
					translationInMicrometer, // translation in micrometer
					new double[]{ 1.0, 1.0, 1.0}, // Bdv Transformations are in our case in micrometer units
				paraFullResImageCentreInMicrometer );


		affineTransform3DForBdv.concatenate( scale );

		System.out.println( "\nBdvAffineTransformation" );
		System.out.println( TransformConversions.asStringBdvStyle( affineTransform3DForBdv ) );




	}

}
