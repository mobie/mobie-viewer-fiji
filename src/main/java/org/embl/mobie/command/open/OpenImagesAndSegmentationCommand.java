package org.embl.mobie.command.open;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Images and Segmentation..." )
public class OpenImagesAndSegmentationCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path Regex", required = false )
	public File image0;

	@Parameter( label = "Image Path Regex", required = false )
	public File image1;

	@Parameter( label = "Image Path Regex", required = false )
	public File image2;

	@Parameter( label = "Image Path Regex", required = false )
	public File image3;

	@Parameter( label = "Image Path Regex", required = false )
	public File image4;

	@Parameter( label = "Label Mask Path Regex", required = false )
	public File labels;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

//	@Parameter( label = "Label Mask Table Path Regex", required = false )
//	public File table;

	@Override
	public void run()
	{
		final GridType gridType = GridType.Stitched; // TODO: fetch from UI

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image0 != null ) imageList.add( image0.getAbsolutePath() );
		if ( image1 != null ) imageList.add( image1.getAbsolutePath() );
		if ( image2 != null ) imageList.add( image2.getAbsolutePath() );
		if ( image3 != null ) imageList.add( image3.getAbsolutePath() );
		if ( image4 != null ) imageList.add( image4.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels != null ) labelsList.add( labels.getAbsolutePath() );

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );

		try
		{
			new MoBIE( imageList, labelsList, null, gridType, settings );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}