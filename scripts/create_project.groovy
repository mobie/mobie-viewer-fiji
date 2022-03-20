import bdv.img.WarpedSource
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


// Configure all the paths and names
projectDirectory = "/Users/tischer/Desktop/rafael/mobie";
datasetName = "sample3";
uiSelectionGroup = "rafael";
sectionsPath = "/Users/tischer/Desktop/rafael/Sections_B_corrected.czi";
sectionsRoiPath = "/Users/tischer/Desktop/rafael/Sections_ROIs.zip";
sectionThicknessMicrometer = 0.5;


// Initialise the MoBIE project creator
project = new ProjectCreator( new File( projectDirectory ) );
datasets = project.getDatasetsCreator();
datasets.addDataset( datasetName, false );
images = project.getImagesCreator();

// The czi file contains multiple resolution levels.
// The second argument of below function selects the resolution level.
// 0 refers to the highest resolutions level.
// To make testing faster one can choose another level.
imp = MoBIEHelper.openWithBioFormats( sectionsPath, 3 );
imp.getCalibration().pixelDepth = sectionThicknessMicrometer;
imp.show();
IJ.run( imp, "Enhance Contrast", "saturated=0.35" );

// Open the section ROIs.
// Important: The sizes of the ROIs are in pixel units and thus must match the resolution level that is chosen in MoBIEHelper.openWithBioFormats(...).
rm = RoiManager.getRoiManager();
rm.removeAll();
rm.runCommand("open", sectionsRoiPath );

// Loop through the section ROIs.
// Process them and add them to the MoBIE project.
for ( sectionIndex = 0; sectionIndex < rm.getCount(); sectionIndex++ )
{
	sectionCrop = cropAndClearOutside( imp, rm.getRoi( sectionIndex ) );

	// Shift the sections along the z-axis
	affineTransform3D = new AffineTransform3D();
	affineTransform3D.translate( new double[]{ 0, 0, sectionIndex * sectionThicknessMicrometer } );

	// Add each channel to MoBIE.
	// In MoBIE one image can have only one channel.
	channels = ChannelSplitter.split( sectionCrop );
	for ( c = 0; c < channels.length; c++ )
	{
		channel = channels[ c ];
		if ( c == 0 )
			IJ.run(channel, "Green", "");
		else
			IJ.run(channel, "Grays", "");
		imageName = "Sections_B_z" + sectionIndex + "_c" + c;
		images.addImage( channel, imageName, datasetName, ImageDataFormat.OmeZarr, ProjectCreator.ImageType.image, affineTransform3D, uiSelectionGroup );
	}
}


// Here you can add more images to the project!
// ....

// Show the project in MoBIE
new MoBIE( projectDirectory );

def cropAndClearOutside( ImagePlus imp, Roi roi )
{
	tmp = new Duplicator().run( imp, 1, imp.getNChannels(), 1, 1, 1, 1 );
	tmp.show();
	tmp.setRoi( roi );
	IJ.setBackgroundColor( 0, 0, 0 );
	IJ.run( tmp, "Clear Outside", "stack" );
	IJ.run( "Crop" );
	crop = IJ.getImage();
	IJ.run(crop, "Select None", "");
	return crop;
}
