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
package users.rafael;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.create.DatasetsCreator;
import org.embl.mobie.viewer.create.ImagesCreator;
import org.embl.mobie.viewer.create.ProjectCreator;

import java.io.File;
import java.io.IOException;

public class CreateRafaelProject
{
	static { LegacyInjector.preinit(); }

	public static void main( String[] args ) throws IOException, SpimDataException
	{
		// Configure all the paths and names
		final String projectDirectory = "/Users/tischer/Desktop/rafael/mobie";
		final String datasetName = "sample3";
		final String uiSelectionGroup = "rafael";
		final String sectionsPath = "/Users/tischer/Desktop/rafael/Sections_B_corrected.czi";
		final String sectionsRoiPath = "/Users/tischer/Desktop/rafael/Sections_ROIs.zip";
		final double sectionThicknessMicrometer = 0.5;

		// Start ImageJ
		// This will be ignored when running in Fiji
		ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		// The czi file contains multiple resolution levels.
		// The second argument of below function selects the resolution level.
		// 0 refers to the highest resolutions level.
		// To make testing faster one can choose another level.
		final ImagePlus imp = MoBIEHelper.openWithBioFormats( sectionsPath, 3 );
		imp.getCalibration().pixelDepth = sectionThicknessMicrometer;
		imp.show();
		IJ.run( imp, "Enhance Contrast", "saturated=0.35" );

		// Initialise the MoBIE project creator
		final ProjectCreator project = new ProjectCreator( new File( projectDirectory ) );
		final DatasetsCreator datasets = project.getDatasetsCreator();
		datasets.addDataset( datasetName, false );
		final ImagesCreator images = project.getImagesCreator();

		// Open the section ROIs.
		// Important: The sizes of the ROIs are in pixel units and thus must match the resolution level that is chosen in MoBIEHelper.openWithBioFormats(...).
		RoiManager rm = RoiManager.getRoiManager();
		rm.removeAll();
		rm.runCommand("open", sectionsRoiPath );

		// Loop through the section ROIs.
		// Process them and add them to the MoBIE project.
		for ( int sectionIndex = 0; sectionIndex < rm.getCount(); sectionIndex++ )
		{
			final ImagePlus sectionCrop = cropAndClearOutside( imp, rm.getRoi( sectionIndex ) );

			// Shift the sections along the z-axis
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			affineTransform3D.translate( new double[]{ 0, 0, sectionIndex * sectionThicknessMicrometer } );

			// Add each channel to MoBIE.
			// In MoBIE one image can have only one channel.
			ImagePlus[] channels = ChannelSplitter.split( sectionCrop );
			for ( int c = 0; c < channels.length; c++ )
			{
				final String imageName = "Sections_B_z" + sectionIndex + "_c" + c;
				images.addImage( channels[ c ], imageName, datasetName, ImageDataFormat.OmeZarr, ProjectCreator.ImageType.image, affineTransform3D, uiSelectionGroup );
			}
		}

		// Show the project in MoBIE
		new MoBIE( projectDirectory, intensityImage, labelImage, resultsTable );
	}

	private static ImagePlus cropAndClearOutside( ImagePlus imp, Roi roi )
	{
		final ImagePlus tmp = new Duplicator().run( imp, 1, imp.getNChannels(), 1, 1, 1, 1 );
		tmp.show();
		tmp.setRoi( roi );
		IJ.setBackgroundColor( 0, 0, 0 );
		IJ.run( tmp, "Clear Outside", "stack" );
		IJ.run( "Crop" );
		final ImagePlus crop = IJ.getImage();
		IJ.run(crop, "Select None", "");
		return crop;
	}
}
