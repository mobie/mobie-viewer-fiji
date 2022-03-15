package create;

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
import org.embl.mobie.viewer.projectcreator.DatasetsCreator;
import org.embl.mobie.viewer.projectcreator.ImagesCreator;
import org.embl.mobie.viewer.projectcreator.ProjectCreator;

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
		new MoBIE( projectDirectory );
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