/*-
 * #%L
 * Fiji viewer for MoBIE projects
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
package org.embl.mobie.command.context;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.align.TurboReg2DAligner;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.embl.mobie.lib.align.SIFT2DAligner;
import org.embl.mobie.lib.color.opacity.MoBIEColorConverter;
import org.embl.mobie.lib.serialize.transformation.InterpolatedAffineTransformation;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.transform.InterpolatedAffineRealTransform;
import org.embl.mobie.lib.transform.Transform;
import org.embl.mobie.lib.view.ViewManager;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.*;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getWindowCentreInPixelUnits;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Automatic 2D/3D")
public class AutomaticRegistrationCommand extends AbstractRegistrationCommand
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Registration method", choices = {"TurboReg", "SIFT"} )
	private String registrationMethod = "SIFT";

	@Parameter(label = "Registration voxel size", persist = false, min = "0.0", style="format:#.00000")
	public Double voxelSize = 1D;

	@Parameter ( label = "Transformation" )
	private Transform transformationType = Transform.Affine;

	@Parameter ( label = "Compute transformation", callback = "compute")
	private Button compute;

	@Parameter ( label = "Show intermediate images" )
	private Boolean showIntermediates = false;

	@Parameter ( label = "Append transformation to stack", callback = "append")
	private Button append;

	@Parameter ( label = "Preview stack transformed image", callback = "showInterpolatedAffineImage" )
	private Boolean showInterpolatedAffineImage = false;

	@Parameter ( label = "Save stack transformed image", callback = "saveInterpolatedAffineImage" )
	private Button saveInterpolatedAffineImage;

	private AffineTransform3D alignmentTransform;
	private final TreeMap< Double, double[] > transforms = new TreeMap<>();
	private SourceAndConverter< ? > interpolatedTransformsSac;


	@Override
	public void initialize()
	{
		super.initialize();

		getInfo().getMutableInput("voxelSize", Double.class)
				.setValue( this, 2 * BdvHandleHelper.getViewerVoxelSpacing( bdvHandle ) );
	}


	@Override
	public void cancel()
	{
		removeInterpolatedPreview();
	}

	private void compute()
	{
		// remove any previous transformation, because
		// we want to register the input image as is
		if ( previewTransform )
		{
			previewTransform( null, false );
		}

		long start = System.currentTimeMillis();

		SourceAndConverter< ? > fixedSac = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( fixedImageName ) )
				.findFirst().get();


		// create two 2D ImagePlus that are to be aligned
		//
		ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, fixedSac.getSpimSource().getVoxelDimensions().unit() );
		screenShotMaker.run( Arrays.asList( fixedSac, movingSac ), voxelSize );
		CompositeImage compositeImage = screenShotMaker.getCompositeImagePlus();
		ImageStack stack = compositeImage.getStack();

		// set the display ranges and burn them in by converting to uint8
		// this is important for the intensity based registration methods
		ImagePlus fixedImp = new ImagePlus( fixedImageName + " (fixed)", stack.getProcessor( 1 ) );
		compositeImage.setPosition( 1 );
		fixedImp.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
		if ( fixedSac.getConverter() instanceof MoBIEColorConverter &&
			( ( MoBIEColorConverter ) fixedSac.getConverter() ).invert() )
			fixedImp.getProcessor().invert();
		new ImageConverter( fixedImp ).convertToGray8();

		ImagePlus movingImp = new ImagePlus( movingImageName + " (moving)", stack.getProcessor( 2 ) );
		compositeImage.setPosition( 2 );
		movingImp.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
		if ( movingSac.getConverter() instanceof MoBIEColorConverter &&
				( ( MoBIEColorConverter ) movingSac.getConverter() ).invert() )
			movingImp.getProcessor().invert();
		new ImageConverter( movingImp ).convertToGray8();

		// set the rois within which the images contain valid pixel values
		// those are used by some registration methods, e.g. turboReg
		Roi[] rois = screenShotMaker.getMasks();
		fixedImp.setRoi( rois[ 0 ] );
		movingImp.setRoi( rois[ 1 ] );

		// compute the transformation that aligns the two images in 2D
		//
		AffineTransform3D localRegistration = new AffineTransform3D();
		if ( registrationMethod.equals( "SIFT" ) )
		{
			SIFT2DAligner sift2DAligner = new SIFT2DAligner( fixedImp, movingImp, transformationType );
			if ( ! sift2DAligner.run( showIntermediates ) ) return;
			localRegistration = sift2DAligner.getAlignmentTransform();
		}
		else if ( registrationMethod.equals( "TurboReg" ) )
		{
			TurboReg2DAligner turboReg2DAligner = new TurboReg2DAligner( fixedImp, movingImp, transformationType );
			turboReg2DAligner.run( showIntermediates );
			localRegistration = turboReg2DAligner.getAlignmentTransform();
			if ( showIntermediates )
			{
				fixedImp.show();
				movingImp.show();
			}
		}

		// convert the transformation that aligns
		// the images in the 2D screenshot canvas
		AffineTransform3D canvasToGlobalTransform = screenShotMaker.getCanvasToGlobalTransform();
		// to the global 3D coordinate system
		alignmentTransform = new AffineTransform3D();
		// global to target canvas...
		alignmentTransform.preConcatenate( canvasToGlobalTransform.inverse() );
		// ...registration within canvas...
		alignmentTransform.preConcatenate( localRegistration );
		// ...canvas back to global...
		alignmentTransform.preConcatenate( canvasToGlobalTransform );
		// ...and invert (don't ask why).
		alignmentTransform = alignmentTransform.inverse();

		IJ.log( "Computed transform in " + ( System.currentTimeMillis() - start ) + " ms:" );
		IJ.log( MoBIEHelper.print( alignmentTransform.getRowPackedCopy(), 2 ) );

		previewTransform( alignmentTransform, true );
	}


	private void append()
	{
		// compute where in the source stack the current transformation should be anchored
		AffineTransform3D globalToSource = BdvHandleHelper.getSourceTransform( movingSac.getSpimSource(), 0, 0 ).inverse();
		double[] sourceVoxels = new double[ 3 ];

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(viewerTransform);
		double[] canvasCenterVoxels = getWindowCentreInPixelUnits (bdvHandle );
		double[] canvasCenterCalibrated = new double[3];
		viewerTransform.inverse().apply( canvasCenterVoxels, canvasCenterCalibrated );
		globalToSource.apply( canvasCenterCalibrated, sourceVoxels );

		transforms.put( sourceVoxels[ 2 ], alignmentTransform.getRowPackedCopy() );

		IJ.log( new InterpolatedAffineTransformation( "AutomaticRegistration", transforms, null, null).toString() );
	}

	private void showInterpolatedAffineImage( )
	{
		if ( transforms.size() < 2 )
		{
			IJ.showMessage( "Please append transformations for at least at two different z positions." );
			showInterpolatedAffineImage = false;
			return;
		}

		removeInterpolatedPreview();

		if( showInterpolatedAffineImage )
		{
			AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( movingSac.getSpimSource(), 0, 0 );
			InterpolatedAffineRealTransform interpolatedTransform = new InterpolatedAffineRealTransform( "interpolated-affine-of-" + movingImageName, sourceTransform.inverse() );
			interpolatedTransform.addTransforms( transforms );

			RealTransformedSource< ? > realTransformedSource = new RealTransformedSource<>(
					movingSac.asVolatile().getSpimSource(),
					interpolatedTransform,
					movingSac.getSpimSource().getName() + "_iat" // iat = interpolated affine transformed
			);

			interpolatedTransformsSac = BdvFunctions.show(
							realTransformedSource,
							BdvOptions.options().addTo( bdvHandle ) ).getSources().get( 0 );

			if ( movingSac.getConverter() instanceof ColorConverter &&
					interpolatedTransformsSac.getConverter() instanceof ColorConverter )
			{
				ColorConverter newConverter = ( ColorConverter ) interpolatedTransformsSac.getConverter();
				ColorConverter oldConverter = ( ColorConverter ) movingSac.getConverter();
				newConverter.setColor( oldConverter.getColor() );
				newConverter.setMin( oldConverter.getMin() );
				newConverter.setMax( oldConverter.getMax() );
			}

		}
	}

	private void createInterpolatedAffineImage()
	{
		String transformedImageName = movingImageName + "-" + transformedImageSuffixUI(  "interpolated-affine" );

		InterpolatedAffineTransformation interpolatedAffineTransformation = new InterpolatedAffineTransformation(
				"interpolated-affine-of-" + movingImageName,
				transforms,
				movingImageName, // the existing, to be transformed image data source
				transformedImageName
		);

		ViewManager.createTransformedSourceView(
						movingSac,
						transformedImageName,
						interpolatedAffineTransformation,
				"Interpolated affine transformation of " + movingSac.getSpimSource().getName() );

		removeInterpolatedPreview();
	}

	private void removeInterpolatedPreview()
	{
		if ( interpolatedTransformsSac != null )
			bdvHandle.getViewerPanel().state().removeSource( interpolatedTransformsSac );
	}
}
