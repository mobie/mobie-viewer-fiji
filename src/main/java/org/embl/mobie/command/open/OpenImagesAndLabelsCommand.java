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

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Images and Label Masks..." )
public class OpenImagesAndLabelsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image0;

	@Parameter( label = "Image Path", required = false )
	public File image1;

	@Parameter( label = "Image Path", required = false )
	public File image2;

	@Parameter( label = "Image Path", required = false )
	public File image3;

	@Parameter( label = "Image Path", required = false )
	public File image4;

	@Parameter( label = "Label Mask Path", required = false )
	public File labels;

	@Parameter( label = "Label Mask Table Path", required = false )
	public File tables;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

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

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( tables != null ) labelsList.add( tables.getAbsolutePath() );

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );

		try
		{
			new MoBIE( imageList, labelsList, tablesList, null, gridType, settings );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}