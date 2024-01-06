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

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.align.TurboReg2DAligner;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.embl.mobie.lib.align.SIFT2DAligner;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.transform.Interpolated3DAffineRealTransform;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Automatic 2D/3D")
public class AutomaticRegistrationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Interactive, Initializable
{
	// TODO: create enum for this!
	public static final String TRANSLATION = "Translation";
	public static final String RIGID = "Rigid";
	public static final String SIMILARITY = "Similarity";
	public static final String AFFINE = "Affine";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	public BdvHandle bdvHandle;

	@Parameter ( label = "Registration Method", choices = {"TurboReg", "SIFT"} )
	private String registrationMethod = "SIFT";

	@Parameter(label="Registration Voxel Size", persist = false, min = "0.0", style="format:#.00000")
	public Double voxelSize = 1D;

	@Parameter ( label = "Transformation", choices = { TRANSLATION, RIGID, SIMILARITY, AFFINE } )
	private String transformationType = TRANSLATION;

	@Parameter ( label = "Image A (fixed)", choices = {""} )
	private String imageA;

	@Parameter ( label = "Image B (moving)", choices = {""} )
	private String imageB;

	@Parameter ( label = "Show Intermediates" )
	private Boolean showIntermediates = false;

	@Parameter ( label = "Compute Transformation", callback = "compute")
	private Button compute;

	@Parameter ( label = "Apply Transformation", callback = "apply")
	private Button apply;

	@Parameter ( label = "Restore Transformation", callback = "remove")
	private Button remove;

	// FIXME: The below to buttons are very special and maybe should
	//  only be visible in an child class
	@Parameter ( label = "Store Transformation", callback = "store")
	private Button store;

	@Parameter ( label = "Apply Stored Transformations", callback = "applyStored", description = "FIXME")
	private Button applyStored;


	private AffineTransform3D initialTransform;
	private AffineTransform3D alignmentTransform;
	private TransformedSource< ? > movingSource;
	private List< SourceAndConverter< ? > > sourceAndConverters;
	private TreeMap< Double, AffineTransform3D > transforms= new TreeMap<>();
	private SourceAndConverter< ? > transformedSac;


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

		getInfo().getMutableInput( "imageA", String.class )
				.setChoices( imageNames );

		getInfo().getMutableInput( "imageB", String.class )
				.setChoices( imageNames );

		getInfo().getMutableInput( "imageB", String.class )
				.setValue( this, imageNames.get( 1 ) );

		getInfo().getMutableInput("voxelSize", Double.class)
				.setValue( this, 2 * BdvHandleHelper.getViewerVoxelSpacing( bdvHandle ) );
	}

	@Override
	public void run()
	{
		//
	}

	private void compute()
	{
		long start = System.currentTimeMillis();

		SourceAndConverter< ? > sacA = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( imageA ) )
				.findFirst().get();

		SourceAndConverter< ? > sacB = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( imageB ) )
				.findFirst().get();

		if ( ! ( sacB.getSpimSource() instanceof TransformedSource ) )
		{
			IJ.log("Cannot apply transformations to image of type " + sacB.getSpimSource().getClass() );
			return;
		}

		// create two 2D ImagePlus that are to be aligned
		//
		ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, sacA.getSpimSource().getVoxelDimensions().unit() );

		screenShotMaker.run( Arrays.asList( sacA, sacB ), voxelSize );
		CompositeImage compositeImage = screenShotMaker.getCompositeImagePlus();
		AffineTransform3D canvasToGlobalTransform = screenShotMaker.getCanvasToGlobalTransform();

		ImageStack stack = compositeImage.getStack();
		ImagePlus impA = new ImagePlus( imageA + " (fixed)", stack.getProcessor( 1 ) );
		ImagePlus impB = new ImagePlus( imageB + " (moving)", stack.getProcessor( 2 ) );

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

		movingSource = ( TransformedSource< ? > ) sacB.getSpimSource();
		initialTransform = new AffineTransform3D();
		movingSource.getFixedTransform( initialTransform );

		IJ.log( "Computed transform in " + ( System.currentTimeMillis() - start ) + " ms:" );
		IJ.log( MoBIEHelper.print( alignmentTransform.getRowPackedCopy(), 2 ) );
	}

	private void apply()
	{
		if ( alignmentTransform == null )
		{
			IJ.showMessage( "Please first [ Compute Alignment ]." );
			return;
		}

		AffineTransform3D transform3D = initialTransform.copy();
		transform3D.preConcatenate( alignmentTransform );
		movingSource.setFixedTransform( transform3D );
		bdvHandle.getViewerPanel().requestRepaint();
	}

	private void store()
	{
		double[] windowCentre = BdvHandleHelper.getWindowCentreInCalibratedUnits( bdvHandle );
		transforms.put( windowCentre[ 2 ], alignmentTransform );
		IJ.log( "Stored Transformations:" );
		for ( Map.Entry< Double, AffineTransform3D > entry : transforms.entrySet() ) {
			IJ.log( "Z = " + entry.getKey().toString() + ", T = " + MoBIEHelper.print( entry.getValue().getRowPackedCopy(), 3 ) );
		}
	}

	private void applyStored()
	{
		if ( transformedSac != null )
			bdvHandle.getViewerPanel().state().removeSource( transformedSac );

		Interpolated3DAffineRealTransform transform = new Interpolated3DAffineRealTransform( 1.0 );
		for ( Map.Entry< Double, AffineTransform3D > entry : transforms.entrySet() ) {
			transform.addTransform( entry.getKey(), entry.getValue().inverse().getRowPackedCopy() );
		}
		// FIXME: add this as an image view to the current data set; maybe just add a button for this.
		String transformedSourceName = movingSource.getName() + "_transformed";
		RealTransformedSource< ? > realTransformedSource = new RealTransformedSource<>( movingSource, transform, transformedSourceName );
		transformedSac = BdvFunctions.show( realTransformedSource, BdvOptions.options().addTo( bdvHandle ) )
				.getSources().get( 0 );
	}

	private void remove()
	{
		if ( alignmentTransform == null )
		{
			IJ.showMessage( "Please first [ Compute Alignment ]." );
			return;
		}
		movingSource.setFixedTransform( initialTransform );
		bdvHandle.getViewerPanel().requestRepaint();
	}

}
