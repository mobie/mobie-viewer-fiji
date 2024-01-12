/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.align.TurboReg2DAligner;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.embl.mobie.lib.align.SIFT2DAligner;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.InterpolatedAffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.transform.Interpolated3DAffineRealTransform;
import org.embl.mobie.lib.transform.Transform;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.*;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getWindowCentreInPixelUnits;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Automatic 2D/3D")
public class AutomaticRegistrationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Interactive, Initializable
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	public BdvHandle bdvHandle;

	@Parameter ( label = "Registration Method", choices = {"TurboReg", "SIFT"} )
	private String registrationMethod = "SIFT";

	@Parameter(label="Registration Voxel Size", persist = false, min = "0.0", style="format:#.00000")
	public Double voxelSize = 1D;

	@Parameter ( label = "Transformation" )
	private Transform transformationType = Transform.Affine;

	@Parameter ( label = "Fixed Image", choices = {""} )
	private String fixedImageName;

	@Parameter ( label = "Moving Image", choices = {""} )
	private String movingImageName;

	@Parameter ( label = "Compute Transformation", callback = "compute")
	private Button compute;

	@Parameter ( label = "Show Intermediate Images" )
	private Boolean showIntermediates = false;

	@Parameter ( label = "Apply Transformation", callback = "apply")
	private Boolean apply = false;

	@Parameter ( label = "Append Transformation to Stack", callback = "append")
	private Button append;

	@Parameter ( label = "Preview Stack Transformed Image", callback = "showInterpolatedAffineImage" )
	private Boolean showInterpolatedAffineImage = false;

	@Parameter ( label = "Save Stack Transformed Image", callback = "saveInterpolatedAffineImage" )
	private Button saveInterpolatedAffineImage;

	private AffineTransform3D alignmentTransform;
	private List< SourceAndConverter< ? > > sourceAndConverters;
	private final TreeMap< Double, double[] > transforms = new TreeMap<>();
	private SourceAndConverter< ? > interpolatedTransformsSac;
	private SourceAndConverter< ? > movingSac;


	@Override
	public void initialize()
	{
		sourceAndConverters = MoBIEHelper.getVisibleSacs( bdvHandle );

		if ( sourceAndConverters.size() < 2 )
		{
			IJ.showMessage( "There must be at least two images visible." );
			return;
		}

		final List< String > imageNames = sourceAndConverters.stream()
				.map( sac -> sac.getSpimSource().getName() )
				.collect( Collectors.toList() );

		getInfo().getMutableInput( "fixedImageName", String.class )
				.setChoices( imageNames );

		getInfo().getMutableInput( "movingImageName", String.class )
				.setChoices( imageNames );

		getInfo().getMutableInput( "movingImageName", String.class )
				.setDefaultValue( imageNames.get( 1 ) );

		getInfo().getMutableInput("voxelSize", Double.class)
				.setValue( this, 2 * BdvHandleHelper.getViewerVoxelSpacing( bdvHandle ) );
	}

	@Override
	public void run()
	{
		removeInterpolatedPreview();
	}

	@Override
	public void cancel()
	{
		removeInterpolatedPreview();
	}

	private void compute()
	{
		long start = System.currentTimeMillis();

		SourceAndConverter< ? > fixedSac = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( fixedImageName ) )
				.findFirst().get();

		movingSac = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( movingImageName ) )
				.findFirst().get();

		// create two 2D ImagePlus that are to be aligned
		//
		ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, fixedSac.getSpimSource().getVoxelDimensions().unit() );

		screenShotMaker.run( Arrays.asList( fixedSac, movingSac ), voxelSize );
		CompositeImage compositeImage = screenShotMaker.getCompositeImagePlus();
		AffineTransform3D canvasToGlobalTransform = screenShotMaker.getCanvasToGlobalTransform();

		ImageStack stack = compositeImage.getStack();
		ImagePlus impA = new ImagePlus( fixedImageName + " (fixed)", stack.getProcessor( 1 ) );
		ImagePlus impB = new ImagePlus( movingImageName + " (moving)", stack.getProcessor( 2 ) );

		// set the display ranges and burn them in by converting to uint8
		// this is important for the intensity based registration methods
		compositeImage.setPosition( 1 );
		impA.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
		new ImageConverter( impA ).convertToGray8();

		compositeImage.setPosition( 2 );
		impB.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
		new ImageConverter( impB ).convertToGray8();

		// set the rois within which the images contain valid pixel values
		// those are used by some registration methods, e.g. turboReg
		Roi[] rois = screenShotMaker.getMasks();
		impA.setRoi( rois[ 0 ] );
		impB.setRoi( rois[ 1 ] );

		// compute the transformation that aligns the two images in 2D
		//
		AffineTransform3D localRegistration = new AffineTransform3D();
		if ( registrationMethod.equals( "SIFT" ) )
		{
			SIFT2DAligner sift2DAligner = new SIFT2DAligner( impA, impB, transformationType );
			if ( ! sift2DAligner.run( showIntermediates ) ) return;
			localRegistration = sift2DAligner.getAlignmentTransform();
		}
		else if ( registrationMethod.equals( "TurboReg" ) )
		{
			TurboReg2DAligner turboReg2DAligner = new TurboReg2DAligner( impA, impB, transformationType );
			turboReg2DAligner.run( showIntermediates );
			localRegistration = turboReg2DAligner.getAlignmentTransform();
			if ( showIntermediates )
			{
				impA.show();
				impB.show();
			}
		}

		// convert the transformation that aligns
		// the images in the 2D screenshot canvas
		// to the global 3D coordinate system
		alignmentTransform = new AffineTransform3D();
		// global to target canvas...
		alignmentTransform.preConcatenate( canvasToGlobalTransform.inverse() );
		// ...registration within canvas...
		alignmentTransform.preConcatenate( localRegistration );
		// ...canvas back to global
		alignmentTransform.preConcatenate( canvasToGlobalTransform );

		IJ.log( "Computed transform in " + ( System.currentTimeMillis() - start ) + " ms:" );
		IJ.log( MoBIEHelper.print( alignmentTransform.getRowPackedCopy(), 2 ) );
	}

	private void apply()
	{
		if ( alignmentTransform == null )
		{
			IJ.showMessage( "Please first [ Compute Alignment ]." );
			apply = false;
			return;
		}

		if ( apply ) {
			DataStore.sourceToImage().get( movingSac ).transform( alignmentTransform );
		}
		else {
			DataStore.sourceToImage().get( movingSac ).transform( alignmentTransform.inverse() );
		}

		bdvHandle.getViewerPanel().requestRepaint();
	}

	private void append()
	{
		AffineTransform3D globalToSource = BdvHandleHelper.getSourceTransform( movingSac.getSpimSource(), 0, 0 ).inverse();
		double[] sourceVoxels = new double[ 3 ];

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(viewerTransform);
		double[] canvasVoxels = getWindowCentreInPixelUnits(bdvHandle);
		double[] canvasCalibrated = new double[3];

		viewerTransform.inverse().apply( canvasVoxels, canvasCalibrated );
		globalToSource.apply( canvasCalibrated, sourceVoxels );
		System.out.println( Arrays.toString( sourceVoxels ));

		canvasVoxels[0] -= 500;
		viewerTransform.inverse().apply( canvasVoxels, canvasCalibrated );
		globalToSource.apply( canvasCalibrated, sourceVoxels );
		System.out.println( Arrays.toString( sourceVoxels ));

		transforms.put( sourceVoxels[ 2 ], alignmentTransform.inverse().getRowPackedCopy() );
		IJ.log( "Transformation Stack:" );
		Set< Map.Entry< Double, double[] > > entries = transforms.entrySet();
		for ( Map.Entry< Double, double[] > entry : entries ) {
			IJ.log( "Z position in source stack: " + MoBIEHelper.print( entry.getKey(), 3 )
					+ "\n  Transform: " + MoBIEHelper.print( entry.getValue(), 3 ) );
		}
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
			Interpolated3DAffineRealTransform interpolatedTransform = new Interpolated3DAffineRealTransform( sourceTransform.inverse() );
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

	private void saveInterpolatedAffineImage()
	{
		Source< ? > source = movingSac.getSpimSource();
		AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( source, 0, 0 );

		ArrayList< Transformation > transformations = new ArrayList<>();
		AffineTransformation< Object > affineTransformation = new AffineTransformation<>( source.getName() + "_affine", sourceTransform, Collections.singletonList( source.getName() ) );
		transformations.add( affineTransformation );

		String transformedImageName = source.getName() + "_iat";
		InterpolatedAffineTransformation< ? > interpolatedAffineTransformation = new InterpolatedAffineTransformation<>(
				transforms,
				source.getVoxelDimensions().dimension( 2 ),
				movingImageName, // the existing to be transformed image data source
				transformedImageName
				);
		transformations.add( interpolatedAffineTransformation );

		ImageDisplay< ? > imageDisplay = new ImageDisplay<>( transformedImageName, transformedImageName );
		imageDisplay.setDisplaySettings( movingSac );

		View view = new View(
				transformedImageName,
				null,
				Collections.singletonList( imageDisplay ),
				transformations,
				null,
				false,
				"Interpolated affine transformation of " + source.getName() );

		MoBIE.getInstance().getViewManager().getViewsSaver().saveViewDialog( view );

		removeInterpolatedPreview();
	}

	private void removeInterpolatedPreview()
	{
		if ( interpolatedTransformsSac != null )
			bdvHandle.getViewerPanel().state().removeSource( interpolatedTransformsSac );
	}
}
